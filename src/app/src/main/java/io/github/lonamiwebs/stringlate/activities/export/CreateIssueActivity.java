package io.github.lonamiwebs.stringlate.activities.export;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InvalidObjectException;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.utilities.RepoHandler;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_XML_CONTENT;

public class CreateIssueActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private RepoHandler mRepo;
    private String mXmlContent;

    private EditText mIssueTitleEditText;
    private EditText mIssueDescriptionEditText;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_issue);

        mSettings = new AppSettings(this);

        mIssueTitleEditText = (EditText)findViewById(R.id.issueTitleEditText);
        mIssueDescriptionEditText = (EditText)findViewById(R.id.issueDescriptionEditText);

        // Retrieve the strings.xml content to be exported
        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mXmlContent = intent.getStringExtra(EXTRA_XML_CONTENT);
        String locale = intent.getStringExtra(EXTRA_LOCALE);

        String display = LocaleString.getDisplay(locale);
        mIssueTitleEditText.setText(getString(R.string.added_x_translation, locale, display));
        mIssueDescriptionEditText.setText(getString(R.string.new_issue_template, locale, display));
    }

    //endregion

    //region Button events

    public void onCreateIssue(final View v) {
        String title = mIssueTitleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "The issue title cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        String description = mIssueDescriptionEditText.getText().toString().trim();
        if (!description.contains("%x")) {
            Toast.makeText(this, "The issue description must contain \"%x\".", Toast.LENGTH_SHORT).show();
            return;
        } else {
            description = description.replace("%x", String.format("```xml\n%s\n```", mXmlContent));
        }
        if (GitHub.gCannotCall()) {
            Toast.makeText(this,
                    R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.creating_issue_ellipsis),
                getString(R.string.creating_issue_long_ellipsis), true);

        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... desc) {
                try {
                    return GitHub.gCreateIssue(mRepo, desc[0], desc[1], mSettings.getGitHubToken());
                } catch (InvalidObjectException ignored) {
                    // This wasn't a GitHub repository. How did we get here?
                    return null;
                }
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                progress.dismiss();
                onIssueCreated(result);
            }
        }.execute(title, description);
    }

    //endregion

    //region Check posted issue

    private void onIssueCreated(JSONObject jsonObject) {
        try {
            if (jsonObject == null) throw new JSONException("Invalid GitHub repository.");

            String postedUrl = jsonObject.getString("html_url");
            finish();
            CreateUrlSuccessActivity.launchIntent(
                    this, getString(R.string.create_issue_success), postedUrl);
        }
        catch (JSONException e) {
            Toast.makeText(this, R.string.create_issue_error, Toast.LENGTH_SHORT).show();
        }
    }

    //endregion
}