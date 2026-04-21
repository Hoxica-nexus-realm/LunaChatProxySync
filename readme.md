# LunaChatProxySync

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**LunaChatProxySync** は、プロキシサーバー（BungeeCordやVelocity等）配下の複数のバックエンドサーバー間で、特定のLunaChatチャンネルのメッセージを同期するためのプラグインです。

---

## 🎯 目的

通常、LunaChatのチャンネルチャットは同一サーバー内のみ、もしくは、プロキシ全体で有効です。  
このプラグインは**特定のチャンネル**（例: グローバルチャンネル、スタッフチャンネルなど）に限定して、プロキシを越えたメッセージ同期を実現します。

- プロキシ側にLunaChat（Bungee版/Velocity移植版等）は**不要**
- バックエンドサーバーのみに導入して動作
- チャンネル単位で転送を制御できるため、サーバー間で共有したいチャットと分離したいチャットを明確に区別可能

---

## ✨ 特徴

- 設定ファイルで指定したLunaChatチャンネルのメッセージを、プロキシ経由で他サーバーに転送
- 転送先では、そのサーバーのLunaChatチャンネルフォーマットに従って表示
- **Japanize**（ローマ字→かな変換,ローマ字→かな変換→漢字変換）機能に対応（LunaChatの設定に依存）
- **NGWordフィルター**にも対応（LunaChatのフィルターが適用された後のメッセージを転送）
- 権限管理は **LuckPerms** を利用（必須ではありません）

---

## 📋 必須プラグイン（バックエンドサーバー）

- [LunaChat](https://github.com/ucchyocean/LunaChat)

> **プロキシサーバー（BungeeCord等）には追加のプラグインは不要です。**  
> *※[注意事項](http://github.com/Hoxica-nexus-realm/LunaChatProxySync#%EF%B8%8F-%E6%B3%A8%E6%84%8F%E4%BA%8B%E9%A0%85)にも記載の通りVelocityの場合は[BungeeBark](https://github.com/RoinujNosde/BungeeBark/)が必要になるかもしれません。*

---

## 📥 ダウンロード

[Releases](https://github.com/Hoxica-nexus-realm/LunaChatProxySync/releases) から最新の JAR ファイルをダウンロードし、機能を利用する**全バックエンドサーバー**の `plugins` フォルダに配置してください。

---

## ⚙️ 設定

初回起動時に `plugins/LunaChatProxySync/config.yml` が生成されます。
以下は生成されるconfig.ymlの内容です(コメント省略済み)
```yaml
sync-channels:
  - "hoge"
  - "fuga"
discord:
  enabled: false
  webhooks: {"hoge":["webhookUrl1","webhookUrl2"],"fuga":["webhookUrl"]}
  allow-mentions: false
advanced:
  debug-mode: false
  chat-event-priority: LOWEST
```

### 設定項目の説明

- **sync-channels**: 同期対象のチャンネル名をリスト形式で指定します。  
  例: `"global"` チャンネルを同期対象にすると、どのサーバーで `global` に投稿されたメッセージもプレイヤーが存在する全サーバーの `global` チャンネルに転送されます。
- **discord**: Discordへのチャット転送の項目です。
  * **enabled**: Discordへのチャット転送の有効/無効を切り替えます。
  * **webhooks**: 転送するLunaChatチャンネルと転送に使用するWebhookのURLを記述します。一つのLunaChatチャンネルに対し複数のWebhookを設定することも可能です。
  * **allow-mentions**: Webhook転送時に@everyone, @here, およびすべてのロール/ユーザーへのメンションを許可するかを切り替えます。
- **adcanced**: 高度な設定です。
  * **debug-mode**: 転送がうまく動作しない場合などに有効にすると、デバッグ情報がログに出力されます。
  * **chat-event-priority**: チャットイベントを処理する順番を設定します。LunaChatよりも早い段階でLunaChatProxySyncがチャットイベントを取得する必要があります。

> **注意**: 同期対象のチャンネルは**各バックエンドサーバーに存在している必要があります**。  
> 存在しないチャンネルが指定されている場合、メッセージは転送されません。  
> プライベートチャットはプラグインがインストールされているすべてのサーバーに対して有効です。  
> メッセージを転送するには少なくとも転送元・転送先ともに一人以上のプレイヤーが必要です。  

---

## 🎮 コマンド

| コマンド | 説明 | 権限       |
|----------|------|----------|
| `/lcps help` | ヘルプを表示 | lcps.use |
| `/lcps reload` | 設定ファイルをリロード | lcps.use |
| `/tell`, `/t` | クロスサーバーDM | lcps.tell |
| `/message`, `/msg`, `/m` | クロスサーバーDM | lcps.message |
| `/reply`, `/r` | クロスサーバーDMのリプライ | lcps.reply |

---

## 🔑 権限

| 権限ノード      | 説明 | デフォルト |
|------------|------|------------|
| `lcps.use` | リロードなどの管理コマンドを実行可能 | op |
| `lcps.tell`, `lcps.message` | クロスサーバーDM用コマンドを実行可能 | true |
| `lcps.reply` | クロスサーバーDMのリプライ用コマンドを実行可能 | true |

---

## 🔄 動作の仕組み

1. プレイヤーがLunaChatの特定チャンネル（例: `global`）にメッセージを送信
2. LunaChatProxySyncがチャットイベントをフック
3. メッセージが送信されたチャンネルが `sync-channels` に含まれている場合、PluginMessageChannel を通じてプロキシへ転送
4. 送信元サーバーではLunaChatが通常のチャット処理を実行（Japanize、NGWordフィルター、フォーマット適用）
5. PluginMessageChannel を受信したプロキシがプレイヤーが存在する各バックエンドサーバーに PluginMessageChannel を通じて転送
6. 受信した各サーバーで、そのサーバーのLunaChatのチャンネルのフォーマットに従ってメッセージを表示

### 重要なポイント

- **プロキシ側では特別な処理は行いません**。単なるメッセージの転送のみです。
- **転送先での表示フォーマットは、そのサーバーのLunaChatの設定に完全に依存します**。
- 同期対象チャンネルを限定することで、サーバー固有のローカルチャットと分離できます。

---

## 📝 使用例

### グローバルチャットの同期

```yaml
sync-channels:
  - "global"
```

全サーバーで `global` チャンネルが共有されます。  
プレイヤーはどのサーバーにいても、同じグローバルチャットで会話できます。

### スタッフチャットの同期

```yaml
sync-channels:
  - "staff"
```

スタッフ専用チャンネルを全サーバーで共有。サーバーを跨いだスタッフ連絡が可能になります。

### 複数チャンネルの同期

```yaml
sync-channels:
  - "global"
  - "staff"
  - "trade"
```

複数のチャンネルを用途別に同期。グローバルチャット、スタッフチャット、取引チャットなどをそれぞれ独立して共有できます。

---

## ⚠️ 注意事項

- 同期対象チャンネルは**全サーバーで統一したチャンネル名**を使用してください。
- JapanizeやNGWordフィルターは**各転送先サーバーのLunaChatの設定**が適用されます。
- 大規模サーバーでの使用時は、不要なチャンネルを同期対象に含めないよう注意してください（トラフィック増加の原因になります）。
- すべてを転送できるわけではありません。一部機能は転送および転送先での利用ができません。
- Velocityでは[BungeeBark](https://github.com/RoinujNosde/BungeeBark/)が必要になるかもしれません。

---

## 🐛 バグ報告 / 機能要望

[Issues](https://github.com/Hoxica-nexus-realm/LunaChatProxySync/issues) からお願いします。

---

## 📄 ライセンス

[MIT ライセンス](LICENSE)
