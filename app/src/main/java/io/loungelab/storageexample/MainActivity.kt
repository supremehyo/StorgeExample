package io.loungelab.storageexample

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {

    val READ_REQUEST_CODE = 42
    val DEL_REQUEST_CODE = 44
    val WRITE_REQUEST_CODE = 43


    //https://ddangeun.tistory.com/94 참고


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        bt1.setOnClickListener {
            saveImageViewToGallery()
        }

        bt2.setOnClickListener {
            makeImage_file()
        }

        //다운로드 폴드에는 접근권한 없이 읽어올수있음
        bt3.setOnClickListener {
            val READ_REQUEST_CODE : Int = 42

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                addCategory(Intent.CATEGORY_OPENABLE)	//열 수 있는 파일들만 보고싶을 때 사용
                type = "image/*"	//타입과 일치하는 파일들만 보여줍니다.
            }

            startActivityForResult(intent, READ_REQUEST_CODE)
        }

        //파일 삭제
        bt4.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }

            startActivityForResult(intent, DEL_REQUEST_CODE)
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                Log.i(TAG, "Uri: $uri")   // 1
                dumpImageMetaData(uri)    // 2
                showImage(uri)    // 3
            }
        }

        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                Log.i(TAG, "Uri: $uri")   // 1
                writeImage(uri)   // 2
            }
        }

        if (requestCode == DEL_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                Log.i(TAG, "Uri: $uri")
                deleteFile(uri)   // 1
            }
        }

    }


    // 이미지 uri 로 metaData 정보 얻어오는 함수
    fun dumpImageMetaData(uri: Uri) {
        val cursor: Cursor? = contentResolver.query( uri, null, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))  // 1
                Log.i(TAG, "Display Name: $displayName")

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)  // 2
                val size: String = if (!it.isNull(sizeIndex)) {
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                Log.i(TAG, "Size: $size")
            }
        }
    }

    // uri 로 이미지 띄우는 함수
    private fun showImage(uri: Uri) {
        GlobalScope.launch {    // 1
            val bitmap = getBitmapFromUri(uri)    // 2
            withContext(Dispatchers.Main) {
                testImageView.setImageBitmap(bitmap)    // 3
            }
        }
    }

    // uri 로 이미지 띄우는 함수2
    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val parcelFileDescriptor: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    /////////////////////파일쓰기

    //make image file
    fun makeImage_file(){
        val WRITE_REQUEST_CODE: Int = 43
        val fileName = "NewImage.jpg"   // 1
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {  // 2
            addCategory(Intent.CATEGORY_OPENABLE)   // 3
            type = "image/jpg"    // 4
            putExtra(Intent.EXTRA_TITLE, fileName)   // 5
        }

        startActivityForResult(intent, WRITE_REQUEST_CODE)    // 6
    }


    private fun writeImage(uri: Uri) {
        GlobalScope.launch {
            contentResolver.openFileDescriptor(uri, "w").use {    // 1
                FileOutputStream(it!!.fileDescriptor).use { it ->   // 2
                    writeFromRawData_or_imageview_ToFile(it)    // 3
                    it.close()
                }
            }
            withContext(Dispatchers.Main) {   //  4
                Toast.makeText(applicationContext, "Done writing an image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeFromRawData_or_imageview_ToFile(outStream: FileOutputStream) {
  
        //raw 디렉토리에서 불러와서 저장하기 밑에 이미지뷰 저장을 주석처리하고 얘를 풀면 이 기능으로 사용가능
      //  val imageInputStream = resources.openRawResource(R.raw.dddd)  // 5
        
        //이미지뷰에서 저장하기
        testImageView.setDrawingCacheEnabled(true) //캐시로 만들기
        val bitmap: Bitmap = testImageView.getDrawingCache() // 캐시에서 불러오기
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
        val bitmapdata = bos.toByteArray()
        val bs = ByteArrayInputStream(bitmapdata)
        val imageInputStream = bs
        
        while (true) {
            val data = imageInputStream.read()
            if (data == -1) {
                break
            }
            outStream.write(data)   // 6
        }
        imageInputStream.close()
    }

    //////////////파일 지우기
    private fun deleteFile(uri: Uri) {
        DocumentsContract.deleteDocument(contentResolver, uri)    // 2
        Toast.makeText(applicationContext, "Done deleting an image", Toast.LENGTH_SHORT).show()
    }


    //imageview 에 있는거 갤러리에 저장
    private fun saveImageViewToGallery() {
        testImageView.setDrawingCacheEnabled(true)
        val bitmap: Bitmap = testImageView.getDrawingCache()
        MediaStore.Images.Media.insertImage(
            this.contentResolver,
            bitmap, "asdfsdf.jpg",
            ""
        )
    }

}