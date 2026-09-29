package thanhdnh.ueh.edu.article_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Home: danh sách user (avatar + username) dạng lưới. */
public class MainActivity extends AppCompatActivity {
  private static final String USERS_URL = "https://raw.githubusercontent.com/LanTranIT0001/PhotoApp/refs/heads/master/Users.json";

  public GridView gridview;
  private ProgressBar progressBar;
  private TextView tvProgress;

  private AdapterView.OnItemClickListener onitemclick = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      Intent intent = new Intent(getBaseContext(), DetailActivity.class);
      intent.putExtra("id", gridview.getAdapter().getItemId(position));
      startActivity(intent);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getSupportActionBar().hide();

    gridview = findViewById(R.id.gridview);
    progressBar = findViewById(R.id.progressbar);
    tvProgress = findViewById(R.id.tv_progress);

    new UserData(getBaseContext(), gridview, progressBar, tvProgress).loadData(USERS_URL, this);
    gridview.setOnItemClickListener(onitemclick);
  }
}