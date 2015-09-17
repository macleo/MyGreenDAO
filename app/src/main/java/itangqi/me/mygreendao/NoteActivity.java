package itangqi.me.mygreendao;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import me.itangqi.greendao.Note;
import me.itangqi.greendao.NoteDao;


public class NoteActivity extends ListActivity implements AdapterView.OnItemLongClickListener{
    private EditText editText;
    private Cursor cursor;
    public static final String TAG = "DaoExample";


    String textColumn = NoteDao.Properties.Text.columnName;
    String[] from = {textColumn, NoteDao.Properties.Comment.columnName};
    int[] to = {android.R.id.text1, android.R.id.text2};
    String orderBy = null;
    private ListView mListView = null;
    private PopupWindow mPopupWindow = null;
    private SimpleCursorAdapter adapter = null;
    private Context mContext = null;

    private EditText et_content;
    private TextView tv_origin;
    private Button btn_sure;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        mContext = this;
        orderBy = textColumn + " COLLATE LOCALIZED ASC";
        cursor = getDb().query(getNoteDao().getTablename(), getNoteDao().getAllColumns(), null, null, null, null, orderBy);

        adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, cursor, from,
                to);
        setListAdapter(adapter);

        editText = (EditText) findViewById(R.id.editTextNote);
        mListView = this.getListView();
        mListView.setOnItemLongClickListener(this);
    }

    private NoteDao getNoteDao() {
        // 通过 BaseApplication 类提供的 getDaoSession() 获取具体 Dao
        return ((BaseApplication) this.getApplicationContext()).getDaoSession().getNoteDao();
    }

    private SQLiteDatabase getDb() {
        // 通过 BaseApplication 类提供的 getDb() 获取具体 db
        return ((BaseApplication) this.getApplicationContext()).getDb();
    }

    /**
     * Button 点击的监听事件
     *
     * @param view
     */
    public void onMyButtonClick(View view) {
        switch (view.getId()) {
            case R.id.buttonAdd:
                addNote();
                break;
            case R.id.buttonQuery:
                search();
                break;
            default:
                ToastUtils.show(getApplicationContext(), "What's wrong ?");
                break;
        }
    }

    private void addNote() {
        String noteText = editText.getText().toString();
        editText.setText("");

        final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        String comment = "Added on " + df.format(new Date());

        if (noteText == null || noteText.equals("")) {
            ToastUtils.show(getApplicationContext(), "Please enter a note to add");
        } else {
            // 插入操作，简单到只要你创建一个 Java 对象
            Note note = new Note(null, noteText, comment, new Date());
            getNoteDao().insert(note);
            Log.d(TAG, "Inserted new note, ID: " + note.getId());
            cursor.requery();
        }

    }

    private void search() {
        String noteText = editText.getText().toString();
        editText.setText("");
        if (noteText == null || noteText.equals("")) {
            ToastUtils.show(getApplicationContext(), "Please enter a note to query");
        } else {
            // Query 类代表了一个可以被重复执行的查询
            Query query = getNoteDao().queryBuilder()
                    .where(NoteDao.Properties.Text.eq(noteText))
                    .orderAsc(NoteDao.Properties.Date)
                    .build();
            // 查询结果以 List 返回
            List notes = query.list();
            ToastUtils.show(getApplicationContext(), "There have " + notes.size() + " records");
        }
        // 在 QueryBuilder 类中内置两个 Flag 用于方便输出执行的 SQL 语句与传递参数的值
        QueryBuilder.LOG_SQL = true;
        QueryBuilder.LOG_VALUES = true;
    }

    /**
     * ListView 的监听事件，用于删除一个 Item
     *
     * @param l
     * @param view
     * @param position
     * @param id
     */
    @Override
    protected void onListItemClick(ListView l, View view, int position, long id) {
        showAllSubject(view,id);
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long id) {

        // 删除操作，你可以通过「id」也可以一次性删除所有
        getNoteDao().deleteByKey(id);
//        getNoteDao().deleteAll();
        ToastUtils.show(getApplicationContext(), "Deleted note, ID: " + id);
        Log.d(TAG, "Deleted note, ID: " + id);
        cursor.requery();
        return false;
    }


    /**
     * 点击more触发的方法，弹出显示所有年级学科
     * @param view
     */
    public void showAllSubject(View view,final long id) {
//        if (null == mPopupWindow) {

            View layout = LayoutInflater.from(this).inflate(
                    R.layout.dialog_modify, null);

            int width = 400;
            int height = 300;

            //设置弹出部分和面积大小
            mPopupWindow = new PopupWindow(layout, width, height, true);

            //设置动画弹出效果
            mPopupWindow.setAnimationStyle(R.style.PopupAnimation);
            et_content = (EditText) layout.findViewById(R.id.et_content);
            tv_origin = (TextView) layout.findViewById(R.id.tv_origin);
            btn_sure = (Button) layout.findViewById(R.id.btn_sure);


            tv_origin.setText(getNoteDao().loadByRowId(id).getText());
            btn_sure.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!TextUtils.isEmpty(et_content.getText().toString())) {
                        Note mNote = getNoteDao().loadByRowId(id);
                        mNote.setText(et_content.getText().toString());
                        getNoteDao().update(mNote);
                        if(mPopupWindow.isShowing()) {
                            mPopupWindow.dismiss();
                            cursor = getDb().query(getNoteDao().getTablename(), getNoteDao().getAllColumns(), null, null, null, null, orderBy);
                            adapter = new SimpleCursorAdapter(mContext, android.R.layout.simple_list_item_2, cursor, from,to);
                            setListAdapter(adapter);
                        }
                    }
                    else{
                        ToastUtils.show(getApplicationContext(), "What's wrong ?");
                    }
                }
            });
            // 设置半透明灰色
            ColorDrawable dw = new ColorDrawable(0x7DC0C0C0);
            mPopupWindow.setBackgroundDrawable(dw);

            mPopupWindow.setClippingEnabled(true);
//        }

        int[] pos = new int[2];
        mListView.getLocationOnScreen(pos);
        mPopupWindow.showAtLocation(mListView, Gravity.CENTER_HORIZONTAL | Gravity.TOP,
                pos[0], pos[1]);
    }
}