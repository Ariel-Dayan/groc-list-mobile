package com.example.groclistapp.data.network.jokes

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.example.groclistapp.data.model.JokeResponse
import com.example.groclistapp.data.network.HttpResponseHandler
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object JokesClient {
    private const val DEFAULT_JOKE = "When Chuck Norris makes a burrito, its main ingredient is real toes."

    private val jokeApiService: JokesApiService by lazy {
        JokesRetrofitClient.retrofit.create(JokesApiService::class.java)
    }

    private fun getFoodRandomJoke(callback: HttpResponseHandler<String>) {
        val call = jokeApiService.getRandomJokeByCategory("food")

        call.enqueue(object : Callback<JokeResponse> {
            override fun onResponse(call: Call<JokeResponse>, response: Response<JokeResponse>) {
                if (response.isSuccessful) {
                    val jokeResponse = response.body()
                    val jokeContent = jokeResponse?.value

                    if (!jokeContent.isNullOrEmpty()) {
                        callback.onComplete(jokeContent)
                    } else {
                        val errorMessage = "Joke content not found"

                        Log.d("Jokes", "Error: $errorMessage")
                        callback.onFailure(Throwable(errorMessage), DEFAULT_JOKE)
                    }
                } else {
                    val errorMessage = "API Response Failed with code ${response.code()}"
                    Log.e("Jokes", "Error: $errorMessage")
                    callback.onFailure(Throwable(errorMessage), DEFAULT_JOKE)
                }
            }

            override fun onFailure(call: Call<JokeResponse>, t: Throwable) {
                val errorMessage = "API Call Failed: ${t.message}"
                Log.e("Jokes", errorMessage)
                callback.onFailure(Throwable(errorMessage), DEFAULT_JOKE)
            }
        })
    }

     fun setJoke(jokeTextView: TextView, jokeProgressBar: ProgressBar) {
         jokeProgressBar.visibility = View.VISIBLE

         getFoodRandomJoke(object: HttpResponseHandler<String> {
            override fun onComplete(data: String) {
                jokeTextView.text = data
                jokeProgressBar.visibility = View.GONE
                jokeTextView.textDirection = View.TEXT_DIRECTION_LTR
                jokeTextView.gravity = Gravity.START
            }

            override fun onFailure(t: Throwable?, default: String?) {
                Log.d("JokesClient", "Failed to fetch joke: $t?.message")
                jokeTextView.text = default
                jokeProgressBar.visibility = View.GONE
                jokeTextView.textDirection = View.TEXT_DIRECTION_LTR
                jokeTextView.gravity = Gravity.START
            }
         })
    }
}
