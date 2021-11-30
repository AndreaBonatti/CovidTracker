package com.example.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.android.synthetic.main.activity_main.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)
        // Fetch the national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.e(TAG, "onResponse $response")
                val nationalData = response.body()
                if(nationalData == null){
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }
                // .reversed() to retrieve data from the holder to the newest
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })

        // Fetch the state data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.e(TAG, "onResponse $response")
                val stateData = response.body()
                if(stateData == null){
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }
                // .reversed() to retrieve data from the holder to the newest
                perStateDailyData = stateData.reversed().groupBy { it.state }
                Log.i(TAG, "Update spinner with state names")
                // TODO: Update spinner with state names
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        // Create a a new SparkAdapter with data
        val adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        // Update radio buttons to select the positive case and max time by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        // Display metric for the most recent date
        updateInfoForDate(dailyData.last())
    }

    private fun updateInfoForDate(covidData: CovidData) {
        tvMetricLabel.text = NumberFormat.getInstance().format(covidData.positiveIncrease)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}