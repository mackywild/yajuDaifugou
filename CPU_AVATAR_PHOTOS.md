# CPU写真アイコンの差し替え

写真配置先:

`android/app/src/main/assets/cpu_avatars/`

ファイル名:

- `cpu_01.jpg`
- `cpu_02.jpg`
- `cpu_03.jpg`
- `cpu_04.jpg`
- `cpu_05.jpg`
- `cpu_06.jpg`
- `cpu_07.jpg`

最大7CPUに1枚ずつ割り当てます。写真が無い番号は従来の生成アイコンへ自動フォールバックします。

推奨画像は正方形 512x512px 前後、顔を中央寄せしたJPEGです。写真を配置した後にAndroid APKを再ビルドしてください。

公開APKへ写真を同梱する場合は、GPSなど不要なEXIFメタデータを削除してから配置することを推奨します。
