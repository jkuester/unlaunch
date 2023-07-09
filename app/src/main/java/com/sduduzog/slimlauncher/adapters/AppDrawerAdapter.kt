package com.sduduzog.slimlauncher.adapters

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.jkuester.unlauncher.datastore.UnlauncherApp
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.datasource.apps.UnlauncherAppsRepository
import com.sduduzog.slimlauncher.ui.main.HomeFragment
import com.sduduzog.slimlauncher.utils.firstUppercase

enum class RowType {
    Header,
    App
}

sealed class AppDrawerRow(val rowType: RowType) {
    data class Item(val app: UnlauncherApp) : AppDrawerRow(RowType.App)

    data class Header(val letter: String) : AppDrawerRow(RowType.Header)
}

class AppDrawerAdapter(
    private val listener: HomeFragment.AppDrawerListener,
    lifecycleOwner: LifecycleOwner,
    appsRepo: UnlauncherAppsRepository
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val regex = Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/? ]")

    private var apps: List<UnlauncherApp> = listOf()
    private var filteredApps: List<AppDrawerRow> =
        listOf() // Map<Char, List<UnlauncherApp>> = emptyMap()
    private var filterQuery = ""

    init {
        appsRepo.liveData().observe(lifecycleOwner) { unlauncherApps ->
            apps = unlauncherApps.appsList.filter { app -> app.displayInDrawer }.toList()
            updateDisplayedApps()
        }
    }

    override fun getItemCount(): Int = filteredApps.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val drawerRow = filteredApps[position]) {
            is AppDrawerRow.Item -> {
                val unlauncherApp = drawerRow.app
                (holder as ItemViewHolder).bind(unlauncherApp)
                holder.itemView.setOnClickListener {
                    listener.onAppClicked(unlauncherApp)
                }
            }

            is AppDrawerRow.Header -> (holder as HeaderViewHolder).bind(drawerRow.letter)
        }
    }

    override fun getItemViewType(position: Int): Int = filteredApps[position].rowType.ordinal


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (RowType.values()[viewType]) {
            RowType.App -> ItemViewHolder(
                inflater.inflate(R.layout.add_app_fragment_list_item, parent, false)
            )

            RowType.Header -> HeaderViewHolder(
                inflater.inflate(R.layout.app_drawer_fragment_header_item, parent, false)
            )
        }
    }


    fun setAppFilter(query: String = "") {
        filterQuery = regex.replace(query, "")
        this.updateDisplayedApps()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDisplayedApps() {
        val filtered = apps.filter { app ->
            regex.replace(app.displayName, "").contains(filterQuery, ignoreCase = true)
        }
        // building a list with each letter and filtered app resulting in a list of
        // [
        // Header<"G">, App<"Gmail">, App<"Google Drive">, Header<"Y">, App<"YouTube">, ...
        // ]
        filteredApps = filtered.groupBy { app -> app.displayName.firstUppercase() }
            .flatMap { entry ->
                listOf(
                    AppDrawerRow.Header(entry.key),
                    *(entry.value.map { AppDrawerRow.Item(it) }).toTypedArray()
                )
            }
        notifyDataSetChanged()
    }

    val searchBoxListener: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            // Do nothing
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Do nothing
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            setAppFilter(s.toString())
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val item: TextView = itemView.findViewById(R.id.aa_list_item_app_name)

        override fun toString(): String {
            return "${super.toString()} '${item.text}'"
        }

        fun bind(item: UnlauncherApp) {
            this.item.text = item.displayName
        }
    }

    inner class HeaderViewHolder(headerView: View) : RecyclerView.ViewHolder(headerView) {
        private val header: TextView = itemView.findViewById(R.id.aa_list_header_letter)

        override fun toString(): String {
            return "${super.toString()} '${header.text}'"
        }

        fun bind(letter: String) {
            header.text = letter
        }
    }
}