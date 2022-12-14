package biz.monro.employee.APICall

import org.json.JSONObject


/** Интерфейс для обработки результатов запроса к серверу API */

interface APICallCallback {

    /** метод, вызываемый в потоке UI для для обработки результата
     * при успешном выполнении запроса */
    fun onAPISucess(callsrc: String, result: JSONObject) {}


    /** метод, вызываемый в потоке UI для для обработки результата
     * в случае, когда при обраобтке запроса  возникли ошибки */
    fun onAPIError(callsrc: String, result: JSONObject) {}


    /** метод, вызываемый в фоновой потоке для обработки результата
     * при успешном выполнении запроса */
    fun onAPISucessAsync(callsrc: String, result: JSONObject) {}

    /** метод для отображения прогресса при выполнении операции */
    fun onAPIProgressUpdate(code: Int, proc: Int) {}
}