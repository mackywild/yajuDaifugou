import AVFoundation
import Foundation

enum GameAudioCue {
    case yajuAvailable
    case yajuSuccess
}

/// 野獣ルール音声再生。
/// AudioResourcesへ音声を置くだけで有効になる。素材がなければ無音で継続する。
final class YajuAudioPlayer {
    private var player: AVAudioPlayer?

    func play(_ cue: GameAudioCue) {
        let resource: String
        switch cue {
        case .yajuAvailable: resource = "yaju_available"
        case .yajuSuccess: resource = "yaju_success"
        }

        guard let url = locate(resource) else { return }
        do {
            let next = try AVAudioPlayer(contentsOf: url)
            next.prepareToPlay()
            next.play()
            player = next
        } catch {
            // 音声は演出のみ。再生失敗でゲーム進行を止めない。
        }
    }

    private func locate(_ name: String) -> URL? {
        for ext in ["mp3", "wav", "m4a"] {
            if let url = Bundle.main.url(forResource: name, withExtension: ext) {
                return url
            }
            if let url = Bundle.main.url(
                forResource: name,
                withExtension: ext,
                subdirectory: "AudioResources"
            ) {
                return url
            }
        }
        return nil
    }
}
