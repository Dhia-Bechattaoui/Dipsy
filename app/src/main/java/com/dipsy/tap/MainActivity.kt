package com.dipsy.tap

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.android.identity.util.UUID
import java.util.Locale

class MainActivity : ComponentActivity() {
    var amount: Long = 0L
    lateinit var tvAmount: TextView
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("pref", MODE_PRIVATE)
        tvAmount = findViewById<TextView>(R.id.amount)
        keyBund()

    }

    fun keyBund(){
        val digitsButtons = mapOf(
            R.id.btn0 to 0,
            R.id.btn1 to 1,
            R.id.btn2 to 2,
            R.id.btn3 to 3,
            R.id.btn4 to 4,
            R.id.btn5 to 5,
            R.id.btn6 to 6,
            R.id.btn7 to 7,
            R.id.btn8 to 8,
            R.id.btn9 to 9,
        )


        for ((id, digit) in digitsButtons){
            findViewById<Button>(id).setOnClickListener {
                addAmmount(digit)
            }
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            amount = 0L
            updateDisplay()
        }

        val btnOK = findViewById<Button>(R.id.btnOK).setOnClickListener {
          if (amount <= 0) {
              Toast.makeText(this, "Please enter the amount", Toast.LENGTH_LONG).show()
          } else {
              val intent = Intent(this, ResultActivity::class.java)
              intent.putExtra("amount", amount)
              intent.putExtra("currency", "QAR")
              startActivity(intent)
          }
        }
    }

    private fun updateDisplay(){
        val qar = amount / 100.0
        tvAmount.text = String.format(Locale.US, "%,.2f", qar)
    }

    fun addAmmount(value: Int){
        val next = (amount * 10) +value
        if(next <= 99_999_99L){
            amount = next
            updateDisplay()
        }

    }


}

