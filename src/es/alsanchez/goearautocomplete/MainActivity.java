package es.alsanchez.goearautocomplete;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity implements View.OnClickListener
{
    private static final int DIALOG_SEARCHING = 1;

    private String[] searchResults;
    private Exception exception;

    private TextView uiQuery;
    private Button uiSearchButton;
    private ListView uiList;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        uiQuery = (TextView) findViewById(R.id.query);
        uiSearchButton = (Button) findViewById(R.id.search_button);
        uiList = (ListView) findViewById(R.id.list);

        uiSearchButton.setOnClickListener(this);
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch(id)
        {
            case DIALOG_SEARCHING:
                return new ProgressDialog(this);
            default:
                return super.onCreateDialog(id);
        }
    }

    @Override
    public void onClick(View view)
    {
        if(view.getId() == uiSearchButton.getId())
        {
            showDialog(DIALOG_SEARCHING);
            new Thread(searchAction).start();
        }
    }

    private final Runnable searchAction = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                searchResults = GoearAutocomplete.search(uiQuery.getText().toString());
                runOnUiThread(displaySearchResults);
            }
            catch (Exception e)
            {
                exception = e;
                runOnUiThread(displayErrorAction);
            }
            finally
            {
                dismissDialog(DIALOG_SEARCHING);
            }
        }
    };

    private final Runnable displaySearchResults = new Runnable()
    {
        @Override
        public void run()
        {
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    MainActivity.this, android.R.layout.simple_list_item_1, searchResults);
            uiList.setAdapter(adapter);
        }
    };

    private final Runnable displayErrorAction = new Runnable()
    {
        @Override
        public void run()
        {
            Toast errorMessage = Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_LONG);
            errorMessage.setGravity(Gravity.CENTER, 0, 0);
            errorMessage.show();
        }
    };
}
