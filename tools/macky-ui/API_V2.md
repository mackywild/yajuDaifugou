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

列: key,label,type:number/boolean（既定は文字列入力）,readonly,editable:boolean/function,required,validate(value,row),format(value,row),tone(value,row),width,fixed,aggregate:sum/avg/count。select(r,c,extend) は現在表示ページの0始まり位置、editCell(id,key,value) は行キー。paste(tsv) は選択位置から全検証。importCSVは全行置換。新規行はnewRow関数を指定。表示用フォーマットと編集値は別。

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

旧部品 MButton/MPanel/MGrid/MMenu/MBadge はそのまま。新規は MComponent(type,options,action event) と名前付き部品 MField/MSelect/MCheckbox/MRadio/MDatePicker/MFilePicker/MForm/MAdvancedGrid/MVirtualGrid/MTabs/MAccordion/MSidebar/MTooltip/MContextMenu/MProgress/MChart/MTree/MCalendar/MWizard/MTimeline/MKanban/MImageViewer/MRichText/MCommandPalette/MRetryPanel。optionsの変更時に再生成。JSON以外の高度な関数を渡す場合は直接APIをVueライフサイクルで使用する。
