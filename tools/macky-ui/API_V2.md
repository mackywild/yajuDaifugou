# Macky UI v2.0 API リファレンス

## 初期化

    import * as api from "@mackywild/macky-ui";
    import "@mackywild/macky-ui/style.css";
    const ui=api.createUI(document.querySelector("#root"));
    const theme=api.applyTheme(document.querySelector("#root"),"business");
    // 画面終了時 theme.restore(); ui.destroy();

v1 APIは DESIGN.md。以下はv2追加API。コールバックは信頼済みアプリコードを渡す。特記なき場合、elementはHTMLElement。DOMを返す関数はremove()、ハンドルを返す関数はdestroy()で破棄する。

## フォーム

|API|引数|戻り値|
|---|---|---|
|createField|type, name, label, value, options, placeholder, hint, disabled, readonly, multiple, accept, rules, onChange|element,input,getValue,setValue,validate,focus,destroy|
|createTextBox|Field options|Field handle|
|createSelect|options:[{label,value,disabled}], value, multiple等|Field handle|
|createCheckbox|value:boolean, label等|Field handle|
|createRadio|options, value|Field handle|
|createDatePicker|value:"YYYY-MM-DD",min,max|Field handle|
|createFilePicker|multiple,accept|Field handle（File[]）|
|createForm|fields,values,submitLabel,onSubmit,onError,onChange|element,getValues,setValues,validate,submit,isBusy,destroy|
|validateValue|value,rules,all|空文字=合格、エラー文字列=不合格|

type: text/email/password/number/date/time/textarea/select/checkbox/radio/checkbox-group/file/search/url/tel/color/range。numberは未入力を空文字、入力値はNumber。rules: required/minLength/maxLength/min/max/pattern/patternMessage/validate(value,all)。選択値は原則文字列、チェックはboolean、複数は配列。submit()はPromise<boolean>。

## グリッド

|API|引数|戻り値|
|---|---|---|
|createAdvancedGrid|rows,columns,rowKey,pageSize,newRow(rows),canEdit(row,col),canDelete(row),onChange(rows,changes),onError|element,getRows,getChanges,isDirty,commit,reset,undo,redo,paste,copy,exportCSV,importCSV,select,editCell,destroy|
|createVirtualGrid|rows,columns,rowHeight,height,overscan,label,onRowAction|element,update(rows),getRenderedCount,destroy|
|virtualWindow|total,scrollTop,rowHeight,viewportHeight,overscan|start,end,before,after|
|createGridStore|rows,{historyLimit}|get,apply,undo,redo,canUndo,canRedo,commit,reset,isDirty,changes|
|parseDelimited|text, separator|文字列の二次元配列|
|encodeDelimited|rows,separator,{spreadsheetSafe}|文字列|
|normalizeCell|raw,column,row|型変換済み値、無効なら例外|

列: key,label,type:number/boolean（既定は文字列入力）,readonly,editable:boolean/function,required,validate(value,row),format(value,row),tone(value,row),width,fixed,aggregate:sum/avg/count。select(r,c,extend) は現在表示ページの0始まり位置、editCell(id,key,value) は行キー。paste(tsv) は選択位置から全検証。importCSVはv2.0.1で既定無効の非同期全行置換。新規行はnewRow関数を指定。表示用フォーマットと編集値は別。

## ナビゲーション・データ・補助部品

|API|主な引数|戻り値|
|---|---|---|
|createTabs|items:[{id,label,content,disabled}],activeId,onChange|element,setActive,getActive,destroy|
|createAccordion|items:[{label,content,open}],multiple|element|
|createSidebar|items,activeId,onSelect,title|element|
|createTooltip|label,text|element|
|createContextMenu|target,{items,onSelect}|element,close,destroy|
|createProgress|value,max,label|element,setValue,destroy|
|createChart|type:bar/line,data:[{label,value}],label|element|
|createTree|items:[{id,label,children,open}],onSelect|element|
|createCalendar|year,month,events:[{date,label}],onSelect(date,events)|element|
|createWizard|steps:[{label,content,validate}],onComplete|element|
|createTimeline|items:[{date,title,description}]|element|
|createKanban|columns:[{id,label}],cards:[{id,columnId,title,description}],onMove|element,getCards,move,destroy|
|createImageViewer|src,alt|element|
|createRichText|value,onChange|element,getValue,setValue,destroy|
|createCommandPalette|items:[{id,label,keywords,disabled}],onSelect|element|
|createRetryPanel|message,task,onSuccess,onError|element|

contentは信頼済みNode/文字列。タブ等では関数でcontentを生成することも可能。createRichText.getValueはプレーンテキスト。Calendarはローカル年月・ISO日付。Chartは単一系列、数値値。

## テーマ・サービス

|API|引数/戻り値|
|---|---|
|themes|6プリセット定義|
|validateTheme(tokens)|検証済みトークン、無効なら例外|
|themeCSS(tokens)|CSS文字列|
|applyTheme(root,nameOrTokens)|restore()ハンドル|
|saveTheme(storage,key,tokens)|検証済みトークン|
|loadTheme(storage,key)|トークンまたはnull|
|singleFlight(task)|並行呼出を抑止するasync関数|
|guardUnsaved(isDirty,{target})|解除関数|
|createShortcutManager(target)|register(key,fn),destroy|
|createNotificationHistory(ui,{limit})|get,clear,destroy|
|mockRequest({delay,mode,timeout,data,signal})|Promise、mode=success/error/timeout|

shortcut例: register("ctrl+s",fn)、同一キー重複は例外。通知履歴はui.toastをラップし、destroyで復元。離脱警告はブラウザ仕様に依存する。

## メタデータ・画面生成

|API|引数/戻り値|
|---|---|
|registry|getDefaults(type)で使用する34種メタデータ|
|getDefaults(type)|既定optionsの独立コピー|
|cloneOptions(value)|安全なJSON複製|
|normalizeScreen(screen)|検査・正規化済み画面定義|
|componentCode(type,options,framework)|JS/Vueコード断片（api,root,uiまたはkit,hは呼び出し側で準備）|
|mountComponent(api,type,options,{ui,onEvent})|element,handle,destroy|
|mountScreen(api,root,screen,{ui,onEvent})|element,destroy|
|screenHTML(screen,framework)|独立HTML（js/vue）|
|screenJS(screen)|npm導入先で使うJS module|
|screenVue(screen)|Vue SFC|

onEvent(name,value,id) のidはmountScreen時。Builder用optionsはJSON専用。API業務コールバックは生成後にアプリが実装する。

## Vue 3

    import * as Vue from "vue";
    import * as api from "@mackywild/macky-ui";
    import {createVueKit} from "@mackywild/macky-ui/vue";
    const ui=api.createUI(root);
    const kit=createVueKit(Vue,ui);
    app.use(kit);

旧部品 MButton/MPanel/MGrid/MMenu/MBadge はそのまま。新規は MComponent(type,options,action event) と名前付き部品 MField/MSelect/MCheckbox/MRadio/MDatePicker/MFilePicker/MForm/MAdvancedGrid/MVirtualGrid/MTabs/MAccordion/MSidebar/MTooltip/MContextMenu/MProgress/MChart/MTree/MCalendar/MWizard/MTimeline/MKanban/MImageViewer/MRichText/MCommandPalette/MRetryPanel。状態保持部品は同じtype/resetKeyのoptions変更で再生成しない。JSON以外の高度な関数を渡す場合は直接APIをVueライフサイクルで使用する。


## v2.0.1 追加・変更API

|API / option|仕様|
|---|---|
|planCSVImport(text,{rows,columns,rowKey,allowCSVImport,canEdit,canAdd,canDelete})|副作用なし。検証済み rows と changes を返す。不正なら例外|
|createAdvancedGrid.allowCSVImport|既定false。trueとconfirmCSVImport関数の両方が必要|
|createAdvancedGrid.confirmCSVImport(diff)|added/updated/deleted/updates(before,after)を受け取り、booleanまたはPromise<boolean>。trueだけ承認|
|createAdvancedGrid.canAdd(row)|CSV新規行は明示true必須。通常の行追加でも指定されていれば適用|
|createAdvancedGrid.canDelete(row)|CSV削除は明示true必須|
|grid.previewCSV(text)|取込計画（変更なし、履歴不変）|
|grid.importCSV(text)|Promise<boolean>。キャンセル/変更なしfalse。エラー/競合はreject|
|grid.copy({spreadsheetSafe=true})|既定は外部表計算ソフト向け。falseは内部raw TSV|
|grid.isBusy()|CSV確認・取込処理中|
|MComponent / 名前付きVue部品 resetKey|状態保持部品の再生成を明示するString/Number。既定0|
|MComponent / MForm submitTask(values)|保存Promiseを返す関数prop。@actionのsubmitは成功通知|
|mountComponent(...,{submitTask})|JSONフォームに信頼済み非同期保存関数を接続|
|mountScreen(...,{submitTask})|submitTask(values,componentId)|
|生成JS mountApp(root,onEvent,submitTask)|第3引数で保存処理を指定|
|生成Vue SFC submitTask prop|submitTask(values,componentId)|

CSV取込の戻り値と既定許可、Vue optionsの更新挙動は破壊的な操作を防止するための意図的な変更。旧仕様より本節を優先する。


## v2.0.2 API追加・動作変更

| API | 仕様 |
|---|---|
| field.setAccess({disabled,readonly}) | 値・FileListを保持し操作可否を部分更新 |
| form.setAccess({disabled,readonly,fields}) | 全体と個別(name)の操作可否を部分更新。全体ロック時は送信不可 |
| grid.setAccess({disabled,readonly,columns,canEdit,canAdd,canDelete,allowCSVImport}) | 値・履歴・行選択を保持して部分更新。columnsはkey一致列のreadonly/editableのみ |
| grid.getSelectedIds() | 全ページの選択行IDのコピー |
| store.peekUndo()/peekRedo()/peekReset() | 履歴適用前の候補行コピー（undo/redoなしの場合null） |
| Vue options.disabled/readonly | 入力部品・フォーム・Advanced Gridでは再生成なしで反映 |
| Vue options.fields[].disabled/readonly / columns[].readonly/editable | 同じname/keyの操作可否のみ動的反映。他の設定はresetKey |
| mountScreen(...,{submitTask}) | 値とcomponentIdを保存関数に伝える接続を修正 |

grid.undo/redo/resetは操作不可時に例外を返す。現時点の権限制約を検査するため、以前に可能だった操作でも権限変更後は履歴適用できない場合がある。権限解除後は履歴を再利用可能。行削除は表示中の選択行に限定。直接APIのsetAccessは部分更新、Vueのoptionsに含まれる操作可否フラグはスナップショットとして扱う。操作禁止はサーバー認可とは独立。
