package com.frabon.rememberthedate.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.frabon.rememberthedate.R
import com.frabon.rememberthedate.data.Event
import com.frabon.rememberthedate.data.EventType
import com.frabon.rememberthedate.databinding.ItemEventBinding
import com.frabon.rememberthedate.databinding.ItemHeaderBinding
import com.frabon.rememberthedate.viewmodels.ViewStyle
import java.util.Calendar
import androidx.recyclerview.widget.RecyclerView.LayoutParams as MarginLayoutParams


private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class EventAdapter(private val onEventClicked: (Event) -> Unit) :
    ListAdapter<UiItem, RecyclerView.ViewHolder>(EventDiffCallback()) {

    var currentViewStyle: ViewStyle = ViewStyle.ALL_MONTHS

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> HeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> EventViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder -> {
                val eventItem = getItem(position) as UiItem.EventItem
                holder.bind(eventItem.event, onEventClicked, currentViewStyle)
            }

            is HeaderViewHolder -> {
                val headerItem = getItem(position) as UiItem.Header
                holder.bind(headerItem.month)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UiItem.Header -> ITEM_VIEW_TYPE_HEADER
            is UiItem.EventItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    class EventViewHolder private constructor(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event, onEventClicked: (Event) -> Unit, style: ViewStyle) {
            val emoji = when (event.type) {
                EventType.BIRTHDAY -> "🎂"
                EventType.ANNIVERSARY -> "❤️"
                EventType.HOLIDAY -> "🎉"
            }
            binding.eventName.text = "$emoji ${event.name}"

            val context = itemView.context
            if (style == ViewStyle.ALL_MONTHS) {

                val margin =
                    context.resources.getDimensionPixelSize(R.dimen.card_margin_vertical_compact)
                val padding =
                    context.resources.getDimensionPixelSize(R.dimen.card_padding_vertical_compact)

                itemView.updateLayoutParams<MarginLayoutParams> {
                    topMargin = margin
                    bottomMargin = margin
                }
                binding.root.updatePadding(top = padding, bottom = padding)
            } else {

                val margin =
                    context.resources.getDimensionPixelSize(R.dimen.card_margin_vertical_normal)
                val padding =
                    context.resources.getDimensionPixelSize(R.dimen.card_padding_vertical_normal)

                itemView.updateLayoutParams<MarginLayoutParams> {
                    topMargin = margin
                    bottomMargin = margin
                }
                binding.root.updatePadding(top = padding, bottom = padding)
            }

            val dayFormatted = event.day.toString().padStart(2, '0')
            val monthFormatted = event.month.toString().padStart(2, '0')
            binding.eventDate.text = "$dayFormatted/$monthFormatted"
            updateDynamicText(event)

            binding.root.setOnClickListener { onEventClicked(event) }
        }

        private fun updateDynamicText(event: Event) {

            if (event.year == null) {
                binding.eventAge.visibility = View.GONE
                return
            }

            val today = Calendar.getInstance()
            val currentYear = today.get(Calendar.YEAR)
            val currentMonth = today.get(Calendar.MONTH) + 1 // Calendar months are 0-11
            val currentDay = today.get(Calendar.DAY_OF_MONTH)

            val years = currentYear - event.year

            val resources = itemView.context.resources
            val dynamicText = when {
                event.month == currentMonth && event.day == currentDay -> {
                    when (event.type) {
                        EventType.BIRTHDAY, EventType.ANNIVERSARY -> resources.getString(
                            R.string.event_item_age_today,
                            years
                        )

                        else -> null
                    }
                }

                event.month < currentMonth || (event.month == currentMonth && event.day < currentDay) -> {
                    when (event.type) {
                        EventType.BIRTHDAY -> resources.getString(
                            R.string.event_item_turned_age,
                            years
                        )

                        EventType.ANNIVERSARY -> resources.getString(
                            R.string.event_item_celebrated_years,
                            years
                        )

                        else -> null
                    }
                }

                else -> {
                    when (event.type) {
                        EventType.BIRTHDAY -> resources.getString(
                            R.string.event_item_turning_age,
                            years
                        )

                        EventType.ANNIVERSARY -> resources.getString(
                            R.string.event_item_celebrating_years,
                            years
                        )

                        else -> null
                    }
                }
            }

            if (dynamicText != null) {
                binding.eventAge.visibility = View.VISIBLE
                binding.eventAge.text = dynamicText
            } else {
                binding.eventAge.visibility = View.GONE
            }
        }

        companion object {
            fun from(parent: ViewGroup): EventViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemEventBinding.inflate(layoutInflater, parent, false)
                return EventViewHolder(binding)
            }
        }
    }

    class HeaderViewHolder private constructor(private val binding: ItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(month: String) {
            binding.headerTitle.text = month
        }

        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemHeaderBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<UiItem>() {
    override fun areItemsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem == newItem
    }
}

sealed class UiItem {
    data class EventItem(val event: Event) : UiItem() {
        override val id = event.id.toLong()
    }

    data class Header(val month: String) : UiItem() {
        override val id = Long.MIN_VALUE + month.hashCode()
    }

    abstract val id: Long
}