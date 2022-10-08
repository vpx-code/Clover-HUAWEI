package com.xvlaze.clover.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class JsonParser {
    private fun parseJsonObject(`object`: JSONObject): HashMap<String, String?> {
        val dataList = HashMap<String, String?>()
        try {
            val name = `object`.getString("name")
            val latitude = `object`.getJSONObject("geometry")
                .getJSONObject("location")
                .getString("lat")
            val longitude = `object`.getJSONObject("geometry")
                .getJSONObject("location")
                .getString("lng")
            val openNow = `object`.getJSONObject("opening_hours")
                .getString("open_now")
            val address = `object`.getString("vicinity")
            dataList["name"] = name
            dataList["lat"] = latitude
            dataList["lng"] = longitude
            dataList["open_now"] = openNow
            dataList["address"] = address
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return dataList
    }

    @Throws(JSONException::class)
    private fun parseJsonArray(jsonArray: JSONArray): List<HashMap<String, String?>> {
        val dataList: MutableList<HashMap<String, String?>> = ArrayList()
        for (i in 0 until jsonArray.length()) {
            val data = parseJsonObject(jsonArray[i] as JSONObject)
            dataList.add(data)
        }
        return dataList
    }

    @Throws(JSONException::class)
    fun parseResult(jsonObject: JSONObject): List<HashMap<String, String?>> {
        return parseJsonArray(jsonObject.getJSONArray("results"))
    }
}