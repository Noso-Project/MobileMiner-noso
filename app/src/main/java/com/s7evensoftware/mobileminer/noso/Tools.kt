package com.s7evensoftware.mobileminer.noso

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.lang.RuntimeException

object Tools {

    fun copyFile(context: Context, assetFilePath: String, localFilePath: String){
        try{
            Log.e("Tools","Copying File from $assetFilePath -> $localFilePath")
            val inputStream = context.assets.open(assetFilePath)
            val outputStream = FileOutputStream(localFilePath)
            var read:Int
            val buffer = ByteArray(4096)
            while (inputStream.read(buffer).also { read = it } > 0) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.close()
            inputStream.close()

            val bin = File(localFilePath)
            bin.setExecutable(true)
        }catch (e:IOException){
            Log.e("Tools","Copying File Filed...")
            throw RuntimeException(e)
        }
    }

    fun copyDirectoryContents(context: Context, assetFilePath:String, localFilePath:String){
        var folder:Array<out String>?
        try {
            folder = context.assets.list(assetFilePath)
        }catch (e:Exception){
            Log.e("Tools","Unable to find asset files")
            return
        }

        if (folder != null) {

            for(f in folder){
                val isDirectory = isAssetDirectory(context, "$assetFilePath/$f")
                if(!isDirectory){
                    val file = File("$localFilePath/$f")
                    if(file.exists() && file.isFile){
                        file.delete()
                    }
                    copyFile(context, "$assetFilePath/$f","$localFilePath/$f")
                }else{
                    val dir = File("$localFilePath/$f")
                    dir.mkdir()
                    copyDirectoryContents(context, "$assetFilePath/$f","$localFilePath/$f")
                }
            }
        }
    }

    fun isAssetDirectory(context: Context, pathInAssetDirectory:String): Boolean {
        var inputStream:InputStream? = null
        var isDirectory = false
        try{
            inputStream = context.assets.open(pathInAssetDirectory)
        }catch (e:IOException){
            isDirectory = true
        }finally {
            inputStream?.close()
        }
        return isDirectory
    }

    fun deleteDirectoryContents(folder: File){
        for(child in folder.listFiles()){
            if(child.isDirectory){
                deleteDirectoryContents(child)
            }

            if(child.isFile){
                child.delete()
            }
        }
    }





}