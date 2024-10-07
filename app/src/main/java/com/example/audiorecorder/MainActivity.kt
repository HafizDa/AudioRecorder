package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : ComponentActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var isRecording = false
    private var audioFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    audioFile = File(externalCacheDir?.absolutePath + "/audioRecord.pcm")

    setContent {
        val coroutineScope = rememberCoroutineScope()
        var recordingState by remember { mutableStateOf("Not Recording") }
        var playbackState by remember { mutableStateOf("Not Playing") }
        var hasPermissions by remember { mutableStateOf(hasPermissions()) }

        LaunchedEffect(Unit) {
            if (!hasPermissions) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = recordingState, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (!isRecording) {
                    recordingState = "Recording..."
                    coroutineScope.launch(Dispatchers.IO) {
                        startRecording()
                    }
                } else {
                    recordingState = "Not Recording"
                    stopRecording()
                }
            }) {
                Text(if (!isRecording) "Start Recording" else "Stop Recording")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                playbackState = "Playing..."
                coroutineScope.launch(Dispatchers.IO) {
                    startPlayback()
                    playbackState = "Not Playing"
                }
            }) {
                Text("Play Recording")
            }
        }
    }
}
    // Function to check if the app has the necessary permissions
    private fun hasPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            return false
        }
        return true
    }

    // Handle the result of the permission request
   override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 1) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "Audio recording permission granted")
        } else {
            Log.d("Permission", "Audio recording permission denied")
        }
    }
}

    // Function to start recording audio
    private fun startRecording() {
        isRecording = true
        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioRecord = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        } else {
            // Permission has already been granted
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        }

        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val audioData = ByteArray(bufferSize)
        audioFile?.let {
            val fos = FileOutputStream(it)
            audioRecord.startRecording()
            while (isRecording) {
                val read = audioRecord.read(audioData, 0, bufferSize)
                fos.write(audioData, 0, read)
            }
            audioRecord.stop()
            fos.close()
        }
    }

    // Function to stop recording
    private fun stopRecording() {
        isRecording = false
    }

    // Function to play back the recorded audio
    private fun startPlayback() {
        val bufferSize = AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        val audioData = ByteArray(bufferSize)
        audioFile?.let {
            val fis = FileInputStream(it)
            audioTrack.play()
            var read: Int
            while (fis.available() > 0) {
                read = fis.read(audioData)
                if (read > 0) {
                    audioTrack.write(audioData, 0, read)
                }
            }
            fis.close()
        }
        audioTrack.stop()
    }
}