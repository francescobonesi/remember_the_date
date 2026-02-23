package com.frabon.rememberthedate.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.frabon.rememberthedate.R
import com.frabon.rememberthedate.RememberTheDateApplication
import com.frabon.rememberthedate.data.Event
import com.frabon.rememberthedate.data.EventType
import com.frabon.rememberthedate.databinding.FragmentAddEditEventBinding
import com.frabon.rememberthedate.viewmodels.AddEditViewModel
import com.frabon.rememberthedate.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditEventFragment : Fragment() {

    private var _binding: FragmentAddEditEventBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditEventFragmentArgs by navArgs()
    private var currentEvent: Event? = null

    private val addEditViewModel: AddEditViewModel by viewModels {
        ViewModelFactory(
            (requireActivity().application as RememberTheDateApplication).repository,
            requireContext().applicationContext
        )
    }

    private var selectedDay: Int = 0
    private var selectedMonth: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.eventId != -1) {
            setupMenu()
            lifecycleScope.launch {
                currentEvent = addEditViewModel.getEventById(args.eventId).first()
                currentEvent?.let { populateUi(it) }
            }
        } else {
            // Default selection for a new event
            binding.radioBirthday.isChecked = true
        }

        binding.dateEditText.setOnClickListener {
            showDatePickerDialog()
        }

        binding.saveButton.setOnClickListener {
            saveEvent()
        }
    }

    private fun populateUi(event: Event) {
        binding.nameEditText.setText(event.name)
        binding.yearEditText.setText(event.year?.toString() ?: "")
        selectedDay = event.day
        selectedMonth = event.month
        binding.dateEditText.setText("$selectedDay/$selectedMonth")

        when (event.type) {
            EventType.BIRTHDAY -> binding.radioBirthday.isChecked = true
            EventType.ANNIVERSARY -> binding.radioAnniversary.isChecked = true
            EventType.HOLIDAY -> binding.radioHoliday.isChecked = true
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Use our custom theme here
        DatePickerDialog(
            requireContext(),
            R.style.Theme_RememberTheDate_DatePicker,
            { _, _, monthOfYear, dayOfMonth ->
                selectedDay = dayOfMonth
                selectedMonth = monthOfYear + 1
                binding.dateEditText.setText("$selectedDay/$selectedMonth")
            },
            year,
            month,
            day
        ).apply {
            // This part hides the year selector in the spinner dialog
            datePicker.findViewById<View>(
                resources.getIdentifier(
                    "year",
                    "id",
                    "android"
                )
            )?.visibility = View.GONE
        }.show()
    }

    private fun saveEvent() {
        val name = binding.nameEditText.text.toString().trim()
        val year = binding.yearEditText.text.toString().toIntOrNull()

        val selectedTypeId = binding.typeRadioGroup.checkedRadioButtonId
        if (name.isBlank() || selectedDay == 0 || selectedTypeId == -1) {
            Toast.makeText(
                context,
                getString(R.string.add_edit_toast_validation_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val eventType = when (selectedTypeId) {
            R.id.radio_anniversary -> EventType.ANNIVERSARY
            R.id.radio_holiday -> EventType.HOLIDAY
            else -> EventType.BIRTHDAY
        }

        val eventToSave = currentEvent?.copy(
            name = name,
            day = selectedDay,
            month = selectedMonth,
            type = eventType,
            year = year
        ) ?: Event(
            name = name,
            day = selectedDay,
            month = selectedMonth,
            type = eventType,
            year = year
        )

        lifecycleScope.launch {
            if (currentEvent == null) {
                addEditViewModel.insertEvent(eventToSave).join()
            } else {
                addEditViewModel.updateEvent(eventToSave).join()
            }
            findNavController().navigateUp()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_confirmation_title))
            .setMessage(getString(R.string.delete_confirmation_message))
            .setPositiveButton(getString(R.string.delete_button)) { _, _ ->
                currentEvent?.let {
                    lifecycleScope.launch {
                        addEditViewModel.deleteEvent(it).join()
                        findNavController().navigateUp()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.edit_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}