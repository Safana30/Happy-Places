package com.example.happyplaces.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity() , View.OnClickListener{

    private var binding:ActivityAddHappyPlaceBinding?=null
    private var cal= Calendar.getInstance()
    lateinit var dateSetListener:DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage:Uri?=null
    private var mLatitude:Double=0.0
    private var mLongitude:Double=0.0

    private var mHappyPlaceDetails:HappyPlaceModel?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails=intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
        }

        dateSetListener=DatePickerDialog.OnDateSetListener {
                view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)

            updateDateInView()
        }

        updateDateInView()

        if(mHappyPlaceDetails!=null){
            supportActionBar?.title="Edit Happy Place"

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLatitude=mHappyPlaceDetails!!.latitude
            mLongitude=mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage= Uri.parse(
                mHappyPlaceDetails!!.image
            )

            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)

            binding?.btnSave?.text="UPDATE"
        }

        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.et_date ->{
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }

            R.id.tv_add_image ->{
                val pictureDialog= AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems=arrayOf("Select photo from Gallery",
                "Capture photo from Camera")
                pictureDialog.setItems(pictureDialogItems){
                    dialog,which->
                    when(which){
                        0-> choosePhotoFromGallery()
                    1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save->{
                when{
                    binding?.etTitle?.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    binding?.etDescription?.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                    }
                    binding?.etLocation?.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage==null->{
                        Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                    }else->{
                        val happyPlaceModel=HappyPlaceModel(
                            if(mHappyPlaceDetails==null) 0 else mHappyPlaceDetails!!.id,
                            binding?.etTitle?.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding?.etDescription?.text.toString(),
                            binding?.etDate?.text.toString(),
                            binding?.etLocation?.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                    val dbHandler=DatabaseHandler(this) //dbHandler object
                    if(mHappyPlaceDetails==null){
                        val addHappyPlace=dbHandler.addHappyPlace(happyPlaceModel)

                        if(addHappyPlace>0){

                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }else{
                        val updateHappyPlace=dbHandler.updateHappyPlace(happyPlaceModel)

                        if(updateHappyPlace>0){

                            setResult(Activity.RESULT_OK)
                            finish()
                        }

                    }

                    }
                }

            }
            R.id.et_location->{
                try {
                    //this is the list of field which has to be passed
                    val fields= listOf(
                        Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    //Start the autocomplete intent with a unique request code.
                    val intent=
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields)
                            .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)


                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK){
            if(requestCode== GALLERY){
                if(data!=null){
                    val contentURI=data.data
                    try {
                        val selectImageBitmap=MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)

                        saveImageToInternalStorage=saveImageToInternalStorage(selectImageBitmap)

                        Log.e("saved image","path: $saveImageToInternalStorage")
                        binding?.ivPlaceImage?.setImageBitmap(selectImageBitmap)
                    }catch (e:IOException){
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity, "Failed to load image from gallery",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }else if(requestCode== CAMERA){
                val thumbnail:Bitmap=data!!.extras!!.get("data")as Bitmap

                saveImageToInternalStorage=saveImageToInternalStorage(thumbnail)

                Log.e("saved image","path: $saveImageToInternalStorage")
                binding?.ivPlaceImage?.setImageBitmap(thumbnail)
            }else if(requestCode== PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place:Place=Autocomplete.getPlaceFromIntent(data!!)
                binding?.etLocation?.setText(place.address)
                mLatitude=place.latLng!!.latitude
                mLongitude=place.latLng!!.longitude
            }
        }
    }

    private fun takePhotoFromCamera(){
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?)
            {if(report!!.areAllPermissionsGranted()){
                val galleryIntent=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(galleryIntent, CAMERA)
            }}
            override fun onPermissionRationaleShouldBeShown(permission: MutableList<PermissionRequest> ,
                                                            token: PermissionToken)
            {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun choosePhotoFromGallery(){
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?)
            {if(report!!.areAllPermissionsGranted()){
                val galleryIntent=Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, GALLERY)
            }}
            override fun onPermissionRationaleShouldBeShown(permission: MutableList<PermissionRequest> ,
                                                            token: PermissionToken)
            {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage(""+
            "It looks like you have turned off permission required for this feature." +
                    "It can be enabled under Application settings "
        ).setPositiveButton("GO TO SETTINGS")
        {_,_ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri= Uri.fromParts("package",packageName,null)
                intent.data=uri
                startActivity(intent)
            }catch (e:ActivityNotFoundException){
                e.printStackTrace()
            }
        }
            .setNegativeButton("Cancel"){dialog,_->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView(){
        val myFormat="dd.MM.yyyy"
        val sdf=SimpleDateFormat(myFormat,Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }
    //return type(Uri)
    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper=ContextWrapper(applicationContext)
        var file=wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file= File(file,"${UUID.randomUUID()}.jpg")

        try{
            val stream:OutputStream=FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e:IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)

    }

    companion object{
        private const val GALLERY=1
        private const val CAMERA=2
        private const val IMAGE_DIRECTORY="HappyPlaceImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE=3

    }
}