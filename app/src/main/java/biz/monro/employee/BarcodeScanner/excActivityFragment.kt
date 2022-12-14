package biz.monro.employee.BarcodeScanner

import com.google.android.gms.vision.CameraSource


interface excActivityFragment {
    fun getBarcode(barc: String, cameraSource: CameraSource, flashmode: Boolean): CameraSource {
        return cameraSource
    }

}