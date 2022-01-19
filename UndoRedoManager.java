package u.n.k.n.o.w.n.b.e.a.s.t;

import android.app.Activity;
import android.content.Context;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import java.util.LinkedList;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.widget.EditText;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
@DesignerComponent(
        version = 1,
        description = "Undo/Redo typed text of a TextBox, by <a htef='https://telegram.me/unknownbeast2166'>UnknownBeast</a>",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "")

@SimpleObject(external = true)
@UsesLibraries(libraries = "")
@UsesPermissions(permissionNames = "")
public class UndoRedoManager extends AndroidNonvisibleComponent {

    private Context context;
    private Activity activity;
    private EditText edtxt;
    private UndoRedoMgr unredo;

    public UndoRedoManager(ComponentContainer container){
        super(container.$form());
        this.activity = container.$context();
        this.context = container.$context();
    }

    @SimpleProperty
    public void TextBoxComponent(AndroidViewComponent view){
    if(view.getView() instanceof EditText){
    edtxt = (EditText) view.getView();
    unredo = new UndoRedoMgr(edtxt);
    }else{
        throw new YailRuntimeError("Error", "Passed Component is not a TextBox");
    }
    }
    @SimpleFunction
    public void Disconnect(){
        if(unredo != null){
    unredo.disconnect();
        }else{
            throw new YailRuntimeError("Error", "UndoRedoManager is not initialized.");
        }
    }
    @SimpleProperty
    public void MaxEditHistorySize(int size){
        if(unredo != null){
    unredo.setMaxHistorySize(size);
        }else{
            throw new YailRuntimeError("Error", "UndoRedoManager is not initialized.");
        }
    }
    @SimpleFunction
    public void ClearEditHistory(){
        if(unredo != null){
    unredo.clearHistory();
        }else{
            throw new YailRuntimeError("Error", "UndoRedoManager is not initialized.");
        }
    }
    @SimpleFunction
    public void UndoLastAction(){
        if(unredo != null){
    unredo.undo();
        }else{
            throw new YailRuntimeError("Error", "UndoRedoManager is not initialized.");
        }
    }
    @SimpleFunction
    public void RedoLastAction(){
        if(unredo != null){
    unredo.redo();
        }else{
            throw new YailRuntimeError("Error", "UndoRedoManager is not initialized.");
        }
    }
    @SimpleFunction
    public boolean CanUndo(){
    return unredo.canUndo();
    }
    @SimpleFunction
    public boolean CanRedo(){
    return unredo.canRedo();
    }

    public static class UndoRedoMgr {

    private boolean mIsUndoOrRedo = false;

    private EditHistory mEditHistory;

    private EditTextChangeListener mChangeListener;

    private EditText editText;

    public UndoRedoMgr(EditText edittext) {
        editText = edittext;
        mEditHistory = new EditHistory();
        mChangeListener = new EditTextChangeListener();
        editText.addTextChangedListener(mChangeListener);
    }

    public void disconnect() {
        editText.removeTextChangedListener(mChangeListener);
    }

    public void setMaxHistorySize(int maxHistorySize) {
        mEditHistory.setMaxHistorySize(maxHistorySize);
    }

    public void clearHistory() {
        mEditHistory.clear();
    }

    public boolean canUndo() {
        return (mEditHistory.mmPosition > 0);
    }

    public void undo() {
        EditItem edit = mEditHistory.getPrevious();
        if (edit == null) {
            return;
        }

        Editable text = editText.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmAfter != null ? edit.mmAfter.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.mmBefore);
        mIsUndoOrRedo = false;
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmBefore == null ? start
                : (start + edit.mmBefore.length()));
    }

    public boolean canRedo() {
        return (mEditHistory.mmPosition < mEditHistory.mmHistory.size());
    }

    public void redo() {
        EditItem edit = mEditHistory.getNext();
        if (edit == null) {
            return;
        }

        Editable text = editText.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmBefore != null ? edit.mmBefore.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.mmAfter);
        mIsUndoOrRedo = false;
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmAfter == null ? start
                : (start + edit.mmAfter.length()));
    }

    public void storePersistentState(Editor editor, String prefix) {
        editor.putString(prefix + ".hash",
                String.valueOf(editText.getText().toString().hashCode()));
        editor.putInt(prefix + ".maxSize", mEditHistory.mmMaxHistorySize);
        editor.putInt(prefix + ".position", mEditHistory.mmPosition);
        editor.putInt(prefix + ".size", mEditHistory.mmHistory.size());

        int i = 0;
        for (EditItem ei : mEditHistory.mmHistory) {
            String pre = prefix + "." + i;

            editor.putInt(pre + ".start", ei.mmStart);
            editor.putString(pre + ".before", ei.mmBefore.toString());
            editor.putString(pre + ".after", ei.mmAfter.toString());

            i++;
        }
    }

    public boolean restorePersistentState(SharedPreferences sp, String prefix)
            throws IllegalStateException {

        boolean ok = doRestorePersistentState(sp, prefix);
        if (!ok) {
            mEditHistory.clear();
        }

        return ok;
    }

    private boolean doRestorePersistentState(SharedPreferences sp, String prefix) {

        String hash = sp.getString(prefix + ".hash", null);
        if (hash == null) {
            return true;
        }

        if (Integer.valueOf(hash) != editText.getText().toString().hashCode()) {
            return false;
        }

        mEditHistory.clear();
        mEditHistory.mmMaxHistorySize = sp.getInt(prefix + ".maxSize", -1);

        int count = sp.getInt(prefix + ".size", -1);
        if (count == -1) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            String pre = prefix + "." + i;

            int start = sp.getInt(pre + ".start", -1);
            String before = sp.getString(pre + ".before", null);
            String after = sp.getString(pre + ".after", null);

            if (start == -1 || before == null || after == null) {
                return false;
            }
            mEditHistory.add(new EditItem(start, before, after));
        }

        mEditHistory.mmPosition = sp.getInt(prefix + ".position", -1);
        if (mEditHistory.mmPosition == -1) {
            return false;
        }

        return true;
    }

    private final class EditHistory {

        private int mmPosition = 0;

        private int mmMaxHistorySize = -1;

        private final LinkedList<EditItem> mmHistory = new LinkedList<EditItem>();

        private void clear() {
            mmPosition = 0;
            mmHistory.clear();
        }

        private void add(EditItem item) {
            while (mmHistory.size() > mmPosition) {
                mmHistory.removeLast();
            }
            mmHistory.add(item);
            mmPosition++;

            if (mmMaxHistorySize >= 0) {
                trimHistory();
            }
        }

        private void setMaxHistorySize(int maxHistorySize) {
            mmMaxHistorySize = maxHistorySize;
            if (mmMaxHistorySize >= 0) {
                trimHistory();
            }
        }

        private void trimHistory() {
            while (mmHistory.size() > mmMaxHistorySize) {
                mmHistory.removeFirst();
                mmPosition--;
            }

            if (mmPosition < 0) {
                mmPosition = 0;
            }
        }

        private EditItem getPrevious() {
            if (mmPosition == 0) {
                return null;
            }
            mmPosition--;
            return mmHistory.get(mmPosition);
        }

        private EditItem getNext() {
            if (mmPosition >= mmHistory.size()) {
                return null;
            }

            EditItem item = mmHistory.get(mmPosition);
            mmPosition++;
            return item;
        }
    }

    private final class EditItem {
        private final int mmStart;
        private final CharSequence mmBefore;
        private final CharSequence mmAfter;

        public EditItem(int start, CharSequence before, CharSequence after) {
            mmStart = start;
            mmBefore = before;
            mmAfter = after;
        }
    }

    private final class EditTextChangeListener implements TextWatcher {

        private CharSequence mBeforeChange;
        private CharSequence mAfterChange;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            if (mIsUndoOrRedo) {
                return;
            }

            mBeforeChange = s.subSequence(start, start + count);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            if (mIsUndoOrRedo) {
                return;
            }

            mAfterChange = s.subSequence(start, start + count);
            mEditHistory.add(new EditItem(start, mBeforeChange, mAfterChange));
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }
}
}
