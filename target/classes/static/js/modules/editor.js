const EditorModule = (function() {
    let queryEditor;
    
    function init() {
        // Initialize CodeMirror for SQL editor
        queryEditor = CodeMirror.fromTextArea(document.getElementById('queryEditor'), {
            mode: 'text/x-sql',
            theme: 'dracula',
            lineNumbers: true,
            indentWithTabs: true,
            smartIndent: true,
            lineWrapping: true,
            matchBrackets: true,
            autofocus: true
        });
        
        return {
            getEditor: function() {
                return queryEditor;
            },
            getValue: function() {
                return queryEditor.getValue();
            },
            setValue: function(value) {
                queryEditor.setValue(value);
            }
        };
    }
    
    return {
        init: init
    };
})();