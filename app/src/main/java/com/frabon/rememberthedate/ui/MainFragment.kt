package com.frabon.rememberthedate.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.frabon.rememberthedate.R
import com.frabon.rememberthedate.RememberTheDateApplication
import com.frabon.rememberthedate.data.EventType
import com.frabon.rememberthedate.databinding.FragmentMainBinding
import com.frabon.rememberthedate.viewmodels.MainViewModel
import com.frabon.rememberthedate.viewmodels.ViewModelFactory
import com.frabon.rememberthedate.viewmodels.ViewStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by viewModels {
        ViewModelFactory(
            (requireActivity().application as RememberTheDateApplication).repository,
            requireContext().applicationContext
        )
    }

    private val exportLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            uri?.let { onExportUriReceived(it) }
        }

    private val importLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { onImportUriReceived(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()

        val adapter = EventAdapter { event ->
            val action = MainFragmentDirections.actionMainFragmentToAddEditEventFragment(event.id)
            findNavController().navigate(action)
        }
        binding.recyclerView.adapter = adapter

        mainViewModel.groupedEvents.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        setupViewStyleControls()
        setupMonthNavigation()
        setupFilterControls()

        lifecycleScope.launch {
            mainViewModel.viewStyle.collectLatest { style ->
                (binding.recyclerView.adapter as? EventAdapter)?.let {
                    it.currentViewStyle = style
                    it.notifyItemRangeChanged(0, it.itemCount)
                }
                binding.monthNavigationContainer.visibility =
                    if (style == ViewStyle.SINGLE_MONTH) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            mainViewModel.currentMonthName.observe(viewLifecycleOwner) { name ->
                binding.textCurrentMonth.text = name
            }
        }

        binding.fab.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToAddEditEventFragment(-1)
            findNavController().navigate(action)
        }
    }

    private fun setupFilterControls() {
        binding.chipFilterAll.isChecked = true
        binding.filterChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val filter = when (checkedId) {
                R.id.chip_filter_birthdays -> EventType.BIRTHDAY
                R.id.chip_filter_anniversaries -> EventType.ANNIVERSARY
                R.id.chip_filter_holidays -> EventType.HOLIDAY
                else -> null
            }
            mainViewModel.setTypeFilter(filter)
        }
    }

    private fun setupViewStyleControls() {
        binding.chipAllMonths.isChecked = true
        binding.viewStyleChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_all_months -> mainViewModel.setViewStyle(ViewStyle.ALL_MONTHS)
                R.id.chip_single_month -> mainViewModel.setViewStyle(ViewStyle.SINGLE_MONTH)
            }
        }
    }

    private fun setupMonthNavigation() {
        binding.buttonNextMonth.setOnClickListener { mainViewModel.nextMonth() }
        binding.buttonPrevMonth.setOnClickListener { mainViewModel.previousMonth() }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        mainViewModel.setSearchQuery(newText.orEmpty())
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_export -> {
                        exportLauncher.launch(getString(R.string.default_export_filename))
                        true
                    }

                    R.id.action_import -> {
                        importLauncher.launch(
                            arrayOf(
                                "text/csv",
                                "text/comma-separated-values",
                                "text/plain"
                            )
                        )
                        true
                    }

                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onExportUriReceived(uri: Uri) {
        lifecycleScope.launch {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.let {
                    mainViewModel.exportEventsToCsv(it)
                    Toast.makeText(
                        context,
                        getString(R.string.export_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.export_failed), Toast.LENGTH_SHORT)
                    .show()
                e.printStackTrace()
            }
        }
    }

    private fun onImportUriReceived(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.let {
                mainViewModel.importEventsFromCsv(it)
                Toast.makeText(context, getString(R.string.import_successful), Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}