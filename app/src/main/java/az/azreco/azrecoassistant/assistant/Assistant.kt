package az.azreco.azrecoassistant.assistant

import android.annotation.SuppressLint
import android.util.Log
import az.azreco.azrecoassistant.R
import az.azreco.azrecoassistant.assistant.audioplayer.AudioPlayer
import az.azreco.azrecoassistant.assistant.azreco.SpeechRecognizer
import az.azreco.azrecoassistant.assistant.azreco.TextToSpeech
import az.azreco.azrecoassistant.assistant.azreco.TextToSpeechModel
import java.io.ByteArrayOutputStream

class Assistant(
    private val speechRecognizer: SpeechRecognizer,
    private val textToSpeech: TextToSpeech,
//    private val exoPlayer: ExoPlayer,
    private val audioPlayer: AudioPlayer
) {
    private val TAG = "DialogFlow"
    private val repeatAudios = listOf(R.raw.repeat_1, R.raw.repeat_2)

    // SPEECH TO TEXT
    suspend fun listen(silence: Int = 3, keyWords: String) =
        speechRecognizer.listen(silence = silence, keyWords = keyWords)

    suspend fun listenSpeech(silence: Int = 3) = speechRecognizer.listenSpeech(silence = silence)

    suspend fun listenKeyword(silence: Int = 5, keyWords: String) =
        speechRecognizer.listenKeyword(silence = silence, keyWords = keyWords)


    suspend fun reListenKeyword(
        keyWords: String,
        limit: Int = 5,
        startLambda: () -> Unit,
        endLambda: suspend (String) -> Unit
    ) {
        var counter = 0
        break_input@ do {
            counter += 1
            startLambda() // callback listening
            val kWord = listenKeyword(keyWords = keyWords, silence = 4)
            if (kWord.isNotEmpty()) {
                Log.d(TAG, "response is not Empty")
                endLambda(kWord)
            } else if (counter >= limit) {
                Log.d(TAG, "counter >= limit")
                playAudio(audioFile = R.raw.signal_stop)
                break@break_input
            } else if (kWord.isEmpty()) {
                Log.d(TAG, "response is Empty")
                playAudio(audioFile = repeatAudios.random())
            }
        } while (kWord.isEmpty())
    }

    suspend fun listenKeyword(
        keyWords: String,
        limit: Int = 10,
        startListen: () -> Unit,
        endListen: suspend (String) -> Unit
    ) {
        var counter = 1
        break_point@ do {
            startListen.invoke()
            var response = listenKeyword(keyWords = keyWords)
            when {
                response.isNotEmpty() -> endListen(response)
                counter >= limit -> {
                    Log.d(TAG, "Limit!")
                    playAudio(audioFile = R.raw.signal_stop)
                    break@break_point
                }
                response.isEmpty() -> {
                    Log.d(TAG, "response is Empty")
                    counter += 1
                    playAudio(audioFile = repeatAudios.random())
                }
            }
            Log.d(TAG, "counter $counter")
            response = ""
        } while (response.isEmpty())
    }

    // TEXT TO SPEECH
    /**
     * play single text by TextToSpeech
     */
    @SuppressLint("NewApi")
    fun speak(text: String, voiceId: String = "") =
        textToSpeech.speak(text = text, voiceId = voiceId)

    /**
     * play ByteArrayOutputStream, generated by TextToSpeech
     */
    @SuppressLint("NewApi")
    fun speakByteStream(baos: ByteArrayOutputStream) =
        textToSpeech.speakByteStream(baos = baos)

    /**
     * synthesize list of texts, returns list of generated ByteArrayOutputStream. for play use speakByteStream
     */
    @SuppressLint("NewApi")
    suspend fun synthesizeMultiple(
        texts: List<String>,
        voiceId: String = ""
    ): List<TextToSpeechModel> = textToSpeech.synthesizeMultiple(texts = texts, voiceId = voiceId)


//    // EXO PLAYER
//    /**
//     * plays ExoPlayer Synchronously,
//     * audioFile - name of asset file without path,
//     * playAltFile - if true, will play postFile after audioFile,
//     * postFile - name of asset file without path
//     */
//    suspend fun playExoSync(
//        audioFile: String,
//        playPostFile: Boolean = true,
//        postFile: String = "signal_start"
//    ) = exoPlayer.playSync(audioFile = audioFile, playPostFile = playPostFile, postFile = postFile)
//
//
//    /**
//     * plays ExoPlayer Asynchronously,
//     * audioFile - name of asset file without path
//     */
//    suspend fun playExoAsync(audioFile: String) = exoPlayer.playAsync(audioFile = audioFile)
//

    suspend fun playAudio(audioFile: Int) = audioPlayer.play(fileName = audioFile)

    /**
     * Release all classes
     */
    suspend fun release() {
        textToSpeech.release()
        speechRecognizer.release()
        audioPlayer.release()
//        exoPlayer.release()
    }

}