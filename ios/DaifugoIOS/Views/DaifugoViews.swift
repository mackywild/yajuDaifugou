import SwiftUI
import UIKit

private let casinoGreen = Color(red: 0.07, green: 0.34, blue: 0.22)
private let casinoGold = Color(red: 0.72, green: 0.52, blue: 0.12)
private let tableGreen = Color(red: 0.04, green: 0.27, blue: 0.16)

struct RootView: View {
    @StateObject private var viewModel = DaifugoViewModel()

    var body: some View {
        ZStack {
            Color(.systemGroupedBackground).ignoresSafeArea()
            VStack(spacing: 0) {
                HeaderView(viewModel: viewModel)
                MessageStrip(viewModel: viewModel)
                Group {
                    switch viewModel.screen {
                    case .login: LoginView(viewModel: viewModel)
                    case .mainMenu: MainMenuView(viewModel: viewModel)
                    case .multiplayer: LobbyView(viewModel: viewModel)
                    case .cpuSetup: CpuSetupView(viewModel: viewModel)
                    case .room: RoomView(viewModel: viewModel)
                    case .game: GameView(viewModel: viewModel)
                    case .result: ResultView(viewModel: viewModel)
                    }
                }
            }
            if viewModel.loading {
                ProgressView().padding(18).background(.ultraThinMaterial).clipShape(RoundedRectangle(cornerRadius: 16))
            }
            if let cutIn = viewModel.ruleCutIns.first {
                RuleCutInOverlay(cutIn: cutIn) {
                    viewModel.dismissRuleCutIn(cutIn.id)
                }
                .zIndex(100)
            }
        }
        .tint(casinoGreen)
    }
}

private struct HeaderView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 0) {
                Text("♛ DAIFUGO")
                    .font(.headline.bold())
                    .foregroundStyle(casinoGreen)
                Text("v0.4.3 J-BACK")
                    .font(.caption2.bold())
                    .foregroundStyle(casinoGold)
            }
            Spacer()
            if viewModel.roomId != nil {
                Text(viewModel.socketConnected ? "● LIVE" : "○ SYNC")
                    .font(.caption.bold())
                    .padding(.horizontal, 10).padding(.vertical, 6)
                    .background(viewModel.socketConnected ? Color.green.opacity(0.16) : Color.orange.opacity(0.16))
                    .clipShape(Capsule())
            }
        }
        .padding(.horizontal, 18).padding(.vertical, 10)
        .background(Color(.secondarySystemGroupedBackground))
    }
}

private struct MessageStrip: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        if let message = viewModel.errorMessage ?? viewModel.infoMessage {
            Button(action: viewModel.clearMessage) {
                Text(message)
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 18).padding(.vertical, 9)
                    .background(viewModel.errorMessage == nil ? Color.green.opacity(0.14) : Color.red.opacity(0.15))
            }
            .buttonStyle(.plain)
        }
    }
}

private struct LoginView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                TitleBlock("テーブルへようこそ", "マルチプレイとCPU戦【ひとりでイク】を選べます。")
                Panel("接続先") {
                    TextField("http://192.168.1.10:8080", text: $viewModel.serverURL)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                        .keyboardType(.URL).textFieldStyle(.roundedBorder)
                    SecureField("共通パスワード", text: $viewModel.password)
                        .textFieldStyle(.roundedBorder)
                    Button("ログイン", action: viewModel.login)
                        .fontWeight(.bold).frame(maxWidth: .infinity).buttonStyle(.borderedProminent)
                }
                Panel("v0.4.3 J-BACK") {
                    Feature("🌐", "PC / Android / iPhone マルチプレイ")
                    Feature("🤖", "CPU戦【ひとりでイク】")
                    Feature("🧠", "簡単 / 普通 / 難しい / N-GOD")
                    Feature("♛", "特殊ルールを対戦ごとに設定")
                }
            }.padding(20)
        }
    }
}

private struct MainMenuView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                HStack {
                    TitleBlock("メインメニュー", "遊ぶモードを選択してください")
                    Spacer()
                    Button("ログアウト", action: viewModel.logout)
                }

                Button(action: viewModel.openMultiplayer) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("🌐 マルチプレイ").font(.title2.bold())
                        Text("PC・Android・iPhoneの友達と同じ卓で対戦")
                            .font(.subheadline).foregroundStyle(.secondary)
                    }
                    .padding(18).frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }
                .buttonStyle(.plain)

                Button(action: viewModel.openCpuSetup) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("🤖 【ひとりでイク】").font(.title2.bold()).foregroundStyle(casinoGold)
                        Text("CPU人数・難易度・特殊ルールを設定してすぐ対戦")
                            .font(.subheadline).foregroundStyle(.secondary)
                        Text("N-GOD搭載").font(.caption.bold()).foregroundStyle(casinoGreen)
                    }
                    .padding(18).frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.orange.opacity(0.09))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }
                .buttonStyle(.plain)
            }.padding(20)
        }
    }
}

private struct CpuSetupView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    private let difficulties = ["EASY", "NORMAL", "HARD", "N_GOD"]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack {
                    TitleBlock("【ひとりでイク】", "CPU戦の設定")
                    Spacer()
                    Button("戻る", action: viewModel.backToMenu)
                }

                Panel("プレイヤー") {
                    TextField("プレイヤー名", text: $viewModel.playerName)
                        .textFieldStyle(.roundedBorder)
                }

                Panel("CPU設定") {
                    Stepper("CPU人数：\(viewModel.cpuCount)人", value: $viewModel.cpuCount, in: 1...3)
                    Picker("難易度", selection: $viewModel.cpuDifficulty) {
                        ForEach(difficulties, id: \.self) { value in
                            Text(cpuDifficultyLabel(value)).tag(value)
                        }
                    }
                    .pickerStyle(.segmented)
                    if viewModel.cpuDifficulty == "N_GOD" {
                        Text("N-GOD：自己対戦で学習した最強CPU")
                            .font(.caption.bold()).foregroundStyle(casinoGold)
                    }
                }

                Panel("デッキ") {
                    Picker("ジョーカー", selection: $viewModel.jokerCount) {
                        Text("0枚").tag(0)
                        Text("1枚").tag(1)
                        Text("2枚").tag(2)
                    }
                    .pickerStyle(.segmented)
                }

                Panel("特殊ルール") {
                    Toggle("革命", isOn: $viewModel.ruleRevolution)
                    Toggle("8切り", isOn: Binding(
                        get: { viewModel.ruleEightCut },
                        set: { viewModel.setEightCut($0) }
                    ))
                    .disabled(viewModel.ruleYaju)
                    Toggle("マーク縛り", isOn: $viewModel.ruleMarkLock)
                    Toggle("7渡し", isOn: $viewModel.ruleSevenTransfer)
                    Toggle("野獣ルール", isOn: Binding(
                        get: { viewModel.ruleYaju },
                        set: { viewModel.setYajuRule($0) }
                    ))
                    Toggle("Jバック", isOn: $viewModel.ruleJackBack)
                    Toggle("禁止上がり", isOn: $viewModel.ruleForbiddenFinish)
                    if viewModel.ruleYaju {
                        Text("※ 野獣ルール使用時は8切りが必須です")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                }

                Button("ひとりでイク", action: viewModel.startCpuGame)
                    .font(.title3.bold()).frame(maxWidth: .infinity)
                    .buttonStyle(.borderedProminent)
            }.padding(20)
        }
    }
}

private struct LobbyView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack {
                    TitleBlock("マルチプレイ", viewModel.serverURL)
                    Spacer()
                    Button("戻る", action: viewModel.backToMenu)
                }
                Panel("プレイヤー") {
                    TextField("プレイヤー名", text: $viewModel.playerName).textFieldStyle(.roundedBorder)
                }
                Panel("新しいテーブル") {
                    Text("あなたがホストになります。2〜4人集まったら開始できます。")
                    Button("＋ 部屋を作る", action: viewModel.createRoom)
                        .fontWeight(.bold).frame(maxWidth: .infinity).buttonStyle(.borderedProminent)
                }
                Panel("部屋へ参加") {
                    TextField("ルームID", text: $viewModel.roomIdInput)
                        .textInputAutocapitalization(.never).autocorrectionDisabled().textFieldStyle(.roundedBorder)
                    Button("参加する", action: viewModel.joinRoom)
                        .frame(maxWidth: .infinity).buttonStyle(.bordered)
                }
            }.padding(20)
        }
    }
}

private struct RoomView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        if let game = viewModel.gameState {
            ScrollView {
                VStack(spacing: 16) {
                    TitleBlock("対戦ルーム", game.host ? "♛ あなたがホスト" : "参加中")
                    Panel("ルームID") {
                        HStack {
                            Text(game.roomId).font(.title2.bold()).foregroundStyle(casinoGreen)
                            Spacer()
                            Button("コピー") { UIPasteboard.general.string = game.roomId }
                        }
                    }
                    Panel("プレイヤー \(game.players.count)/4") {
                        ForEach(Array(game.players.enumerated()), id: \.element.id) { index, player in
                            HStack {
                                Text("\(index + 1)").font(.headline).frame(width: 34, height: 34)
                                    .background(player.isSelf ? Color.green.opacity(0.15) : Color.gray.opacity(0.12))
                                    .clipShape(Circle())
                                VStack(alignment: .leading) {
                                    Text(player.playerName).bold()
                                    Text(player.isSelf ? "あなた" : "準備完了").font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                            }
                            if index < game.players.count - 1 { Divider() }
                        }
                    }
                    if game.host {
                        Button(game.players.count >= 2 ? "ゲームを開始する" : "あと1人必要です", action: viewModel.startGame)
                            .disabled(game.players.count < 2)
                            .fontWeight(.bold).frame(maxWidth: .infinity).buttonStyle(.borderedProminent)
                    } else {
                        Text("ホストがゲームを開始するまで待機中…").fontWeight(.bold)
                    }
                    Button("部屋から退出", action: viewModel.leaveRoom).buttonStyle(.bordered)
                }.padding(20)
            }
        }
    }
}

private struct GameView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        if let game = viewModel.gameState, let me = game.selfPlayer {
            VStack(spacing: 0) {
                ScrollView {
                    VStack(spacing: 14) {
                        OpponentsView(game: game)
                        TableView(game: game)
                        if game.isMySevenTransfer {
                            Text("7渡し：\(game.sevenTransferTarget?.playerName ?? "隣")へ \(game.sevenTransfer.cardCount)枚選択")
                                .font(.headline).foregroundStyle(casinoGold)
                                .padding(10).frame(maxWidth: .infinity)
                                .background(Color.orange.opacity(0.12)).clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }.padding(14)
                }
                HandArea(game: game, me: me, viewModel: viewModel)
            }
        }
    }
}

private struct OpponentsView: View {
    let game: GameStateDTO
    var body: some View {
        HStack(spacing: 8) {
            ForEach(game.players.filter { !$0.isSelf }) { player in
                VStack(spacing: 4) {
                    Text(player.playerName).font(.caption.bold()).lineLimit(1)
                    if player.cpu {
                        Text("CPU \(cpuDifficultyLabel(player.cpuDifficulty ?? ""))")
                            .font(.caption2.bold()).foregroundStyle(casinoGold)
                    }
                    Text("🂠 × \(player.handCount)").font(.caption)
                    if player.yajuActive { Text("野獣").font(.caption2.bold()).foregroundStyle(casinoGold) }
                    if player.rank != nil { Text("\(player.rank!)位").font(.caption2.bold()) }
                }
                .frame(maxWidth: .infinity).padding(8)
                .background(player.playerId == game.currentPlayerId ? Color.green.opacity(0.18) : Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }
}

private struct TableView: View {
    let game: GameStateDTO

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                if game.revolution { Badge("革命") }
                if let mark = game.lockedMark { Badge("\(markSymbol(mark))縛り") }
                Spacer()
                Text(game.currentPlayer?.playerName.map { "手番：\($0)" } ?? "").font(.caption.bold()).foregroundStyle(.white)
            }
            if game.field.cards.isEmpty {
                Text("場は空です").foregroundStyle(.white.opacity(0.8)).frame(height: 90)
            } else {
                HStack(spacing: -10) {
                    ForEach(Array(game.field.cards.enumerated()), id: \.offset) { _, card in
                        PlayingCard(card: card, selected: false).frame(width: 62, height: 90)
                    }
                }
            }
        }
        .padding(16).frame(maxWidth: .infinity, minHeight: 155)
        .background(tableGreen.gradient).clipShape(RoundedRectangle(cornerRadius: 22))
    }
}

private struct HandArea: View {
    let game: GameStateDTO
    let me: PlayerDTO
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                Text(game.isMyTurn ? "あなたの番です" : "待機中").font(.headline.bold())
                if me.yajuActive { Badge("野獣 \(me.yajuStatus)") }
                Spacer()
                Text("残り \(me.handCount)枚").font(.caption)
            }.padding(.horizontal, 12)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 4) {
                    ForEach(Array(me.hand.enumerated()), id: \.offset) { index, card in
                        PlayingCard(card: card, selected: viewModel.selectedCardIndices.contains(index))
                            .frame(width: 66, height: 96)
                            .offset(y: viewModel.selectedCardIndices.contains(index) ? -10 : 0)
                            .onTapGesture { viewModel.toggleCard(index: index) }
                    }
                }.padding(.horizontal, 12).padding(.top, 12)
            }.frame(height: 120)

            HStack(spacing: 12) {
                Button("パス", action: viewModel.pass)
                    .buttonStyle(.bordered).disabled(!game.isMyTurn || game.isMySevenTransfer)
                Button(game.isMySevenTransfer ? "渡す" : "出す", action: viewModel.playSelected)
                    .fontWeight(.bold).buttonStyle(.borderedProminent)
                    .disabled(!game.isMyTurn || viewModel.selectedCardIndices.isEmpty)
            }.padding(.bottom, 10)
        }
        .padding(.top, 8).background(Color(.secondarySystemGroupedBackground))
    }
}

private struct ResultView: View {
    @ObservedObject var viewModel: DaifugoViewModel

    var body: some View {
        if let game = viewModel.gameState {
            ScrollView {
                VStack(spacing: 18) {
                    TitleBlock("RESULT", "ゲーム終了")
                    Panel("最終順位") {
                        ForEach(game.players.sorted { ($0.rank ?? 999) < ($1.rank ?? 999) }) { player in
                            HStack {
                                Text(player.rank.map { "\($0)位" } ?? "-").font(.title3.bold()).frame(width: 48)
                                Text(player.playerName).font(.headline)
                                Spacer()
                                if player.yajuStatus == "COMPLETED" { Text("やりますねぇ").font(.caption.bold()).foregroundStyle(casinoGold) }
                            }
                            Divider()
                        }
                    }
                    Button("メインメニューへ戻る", action: viewModel.leaveRoom)
                        .frame(maxWidth: .infinity).buttonStyle(.borderedProminent)
                }.padding(20)
            }
        }
    }
}

private struct PlayingCard: View {
    let card: CardDTO
    let selected: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 9).fill(.white)
            RoundedRectangle(cornerRadius: 9).stroke(selected ? casinoGold : Color.black.opacity(0.15), lineWidth: selected ? 3 : 1)
            Text(card.label)
                .font(card.label == "JOKER" ? .caption.bold() : .title3.bold())
                .foregroundStyle(card.isRed ? .red : .black)
        }
        .shadow(color: .black.opacity(0.15), radius: 3, y: 2)
    }
}

private struct Badge: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text).font(.caption2.bold()).padding(.horizontal, 8).padding(.vertical, 4)
            .background(Color.orange.opacity(0.18)).foregroundStyle(casinoGold).clipShape(Capsule())
    }
}

private struct Panel<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content
    init(_ title: String, @ViewBuilder content: @escaping () -> Content) { self.title = title; self.content = content }
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.headline.bold())
            content()
        }
        .padding(16).frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground)).clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct TitleBlock: View {
    let title: String
    let subtitle: String
    init(_ title: String, _ subtitle: String) { self.title = title; self.subtitle = subtitle }
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.title.bold())
            Text(subtitle).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
        }
    }
}

private struct Feature: View {
    let icon: String
    let text: String
    init(_ icon: String, _ text: String) { self.icon = icon; self.text = text }
    var body: some View { HStack { Text(icon); Text(text) } }
}

private func cpuDifficultyLabel(_ value: String) -> String {
    switch value {
    case "EASY": return "簡単"
    case "NORMAL": return "普通"
    case "HARD": return "難しい"
    case "N_GOD": return "N-GOD"
    default: return value
    }
}

private func markSymbol(_ value: String) -> String {
    switch value {
    case "SPADE": return "♠"
    case "HEART": return "♥"
    case "DIAMOND": return "♦"
    case "CLUB": return "♣"
    default: return value
    }
}


private struct RuleCutInOverlay: View {
    let cutIn: RuleCutInNotice
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.82).ignoresSafeArea()
            VStack(spacing: 12) {
                Text(cutIn.kind == .earlyShot ? "早漏" : "J BACK")
                    .font(.headline)
                    .fontWeight(.black)
                    .foregroundStyle(cutIn.kind == .earlyShot ? Color.red : Color.purple)
                Text(cutIn.kind == .earlyShot ? "早すぎるッ！" : "バック気持ちいい")
                    .font(.system(size: 40, weight: .black))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white)
                Text(cutIn.playerName)
                    .font(.subheadline.bold())
                    .foregroundStyle(.white.opacity(0.75))
            }
            .padding(30)
        }
        .task(id: cutIn.id) {
            try? await Task.sleep(nanoseconds: 1_900_000_000)
            onDismiss()
        }
    }
}
