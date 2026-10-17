package com.dipsy.tap

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Locale

class HistoryActivity : ComponentActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvHistory.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadHistory()
    }

    private fun loadHistory() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.api.transactions(limit = 20)
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val items = response.body()!!.items
                    if (items.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rvHistory.adapter = HistoryAdapter(items)
                    }
                } else {
                    tvEmpty.text = "Failed to load history"
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                progressBar.visibility = View.GONE
                tvEmpty.text = "Error loading history"
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    class HistoryAdapter(private val items: List<Transaction>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTxId: TextView = view.findViewById(R.id.tvTxId)
            val tvTxDate: TextView = view.findViewById(R.id.tvTxDate)
            val tvTxAmount: TextView = view.findViewById(R.id.tvTxAmount)
            val tvTxStatus: TextView = view.findViewById(R.id.tvTxStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTxId.text = item.id
            holder.tvTxDate.text = item.createdAt.take(19).replace("T", " ")

            val qar = item.amount / 100.0
            holder.tvTxAmount.text = "QAR " + String.format(Locale.US, "%,.2f", qar)

            holder.tvTxStatus.text = item.status.uppercase(Locale.US)
            when (item.status) {
                "success" -> holder.tvTxStatus.setTextColor(Color.parseColor("#4CAF50"))
                "failed" -> holder.tvTxStatus.setTextColor(Color.parseColor("#F44336"))
                else -> holder.tvTxStatus.setTextColor(Color.parseColor("#FF9800"))
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
