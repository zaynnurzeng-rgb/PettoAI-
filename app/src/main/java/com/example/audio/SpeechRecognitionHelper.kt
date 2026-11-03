package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.model.AppLanguage

class SpeechRecognitionHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {},
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onServiceUnavailable: () -> Unit = {},
    private val onErrorCallback: (Int) -> Unit = {}
) {
    private val TAG = "SpeechRecognitionHelper"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            mainHandler.post { onListeningStateChanged(true) }
                        }

                        override fun onBeginningOfSpeech() {
                            isListening = true
                            mainHandler.post { onListeningStateChanged(true) }
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            mainHandler.post { onRmsChanged(rmsdB) }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                            mainHandler.post { onListeningStateChanged(false) }
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "SpeechRecognizer error: $error")
                            isListening = false
                            mainHandler.post {
                                onListeningStateChanged(false)
                                if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER) {
                                    onServiceUnavailable()
                                }
                                onErrorCallback(error)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            mainHandler.post {
                                onListeningStateChanged(false)
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    onResult(matches[0])
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                mainHandler.post { onPartialResult(matches[0]) }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } else {
                Log.w(TAG, "SpeechRecognizer service is not available on this device")
                onServiceUnavailable()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing SpeechRecognizer", e)
            onServiceUnavailable()
        }
    }

    fun startListening(language: AppLanguage): Boolean {
        var started = false
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                if (speechRecognizer != null) {
                    val localeTag = when (language) {
                        AppLanguage.INDONESIAN -> "id-ID"
                        AppLanguage.ENGLISH -> "en-US"
                        AppLanguage.JAPANESE -> "ja-JP"
                        AppLanguage.SPANISH -> "es-ES"
                    }
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang...")
                    }
                    speechRecognizer?.startListening(intent)
                    isListening = true
                    onListeningStateChanged(true)
                    started = true
                } else {
                    onServiceUnavailable()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                isListening = false
                onListeningStateChanged(false)
                onServiceUnavailable()
            }
        }
        return started
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                isListening = false
                onListeningStateChanged(false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop speech recognition", e)
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to destroy speech recognizer", e)
            }
        }
    }

    companion object {
        fun createSystemSpeechIntent(language: AppLanguage): Intent {
            val localeTag = when (language) {
                AppLanguage.INDONESIAN -> "id-ID"
                AppLanguage.ENGLISH -> "en-US"
                AppLanguage.JAPANESE -> "ja-JP"
                AppLanguage.SPANISH -> "es-ES"
            }
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara dengan avatar...")
            }
        }
    }
}
