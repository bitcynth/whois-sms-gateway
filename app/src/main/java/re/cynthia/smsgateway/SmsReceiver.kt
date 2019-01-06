package re.cynthia.smsgateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.net.URL

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras

        if (extras != null) {
            val sms = extras.get("pdus") as Array<Any>

            for (i in sms.indices) {
                val format = extras.getString("format")

                var smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(sms[i] as ByteArray, format)
                } else {
                    SmsMessage.createFromPdu(sms[i] as ByteArray)
                }

                val phoneNumber = smsMessage.originatingAddress
                val messageText = smsMessage.messageBody.toString()

                val msg = "phonenumber: $phoneNumber\nmessageText: $messageText"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                Log.i("SMSGateway", msg)

                val req = object: StringRequest(Request.Method.POST, webhookURL,
                    Response.Listener<String> { res -> handleResponse(res, phoneNumber) },
                    Response.ErrorListener { err -> Log.e("SMSGatewayWH", "error $err") }
                    ) {
                    override fun getBodyContentType(): String {
                        return "application/json"
                    }
                    @Throws(AuthFailureError::class)
                    override fun getBody(): ByteArray {
                        val params = HashMap<String, String>()
                        params["phone_number"] = phoneNumber
                        params["text"] = messageText
                        return JSONObject(params).toString().toByteArray()
                    }
                }
                val queue = Volley.newRequestQueue(context)
                queue.add(req)
            }
        }
    }

    fun handleResponse(res: String, phoneNumber: String) {
        Log.i("SMSGatewayWH", "got response")
        val text = JSONObject(res).getString("response")
        val manager = SmsManager.getDefault()
        val parts = manager.divideMessage(text)
        manager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
    }

}