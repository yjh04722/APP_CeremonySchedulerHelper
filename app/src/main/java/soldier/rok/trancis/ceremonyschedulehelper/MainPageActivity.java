package soldier.rok.trancis.ceremonyschedulehelper;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static soldier.rok.trancis.ceremonyschedulehelper.MainActivity.auth;

public class MainPageActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ArrayList<ListData> listDataArray;
    public static Context m_Ctxt;
    int iItemCnt = 0;
    int iFinishItemCnt = 0;
    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedtime = 0;
    String testStr1, testStr2, testStr3;
    private ProgressBar spinner;

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.toobar_menu_mainpage, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item){
        //각각의 버튼을 클릭할때의 수행할것을 정의해 준다.
        switch (item.getItemId()){
            case R.id.action_setting:
                Toast.makeText(this, "설정 미구현", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_sign_out:
                auth.SetAuth(false);
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        listDataArray = new ArrayList<ListData>();
        spinner = (ProgressBar)findViewById(R.id.progressBarLoad);
        //get schedules from server
        new GetEidByUid().execute();

        ListView listView = (ListView)findViewById(R.id.list_user_schedule);
        CustomAdapter customAdapter = new CustomAdapter(this, R.layout.custom_list_row, listDataArray);
        listView.setAdapter(customAdapter);
        listView.setOnItemClickListener(this);

        //툴바 기능
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar_mainpage);
        toolbar.setTitle(auth.getNick()+"의 행사 내역");
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);





        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), CeremonyAddActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();


       ListView listView = (ListView)findViewById(R.id.list_user_schedule);
       CustomAdapter customAdapter = new CustomAdapter(this, R.layout.custom_list_row, listDataArray);
       listView.setAdapter(customAdapter);
       listView.setOnItemClickListener(this);
    }

    @Override
    public void onBackPressed() {
        long tempTIme = System.currentTimeMillis();
        long intervalTime = tempTIme - backPressedtime;

        if(0<= intervalTime && FINISH_INTERVAL_TIME >= intervalTime){
            System.exit(0);
        }
        else{
            backPressedtime = tempTIme;
            Toast.makeText(getApplicationContext(),"'뒤로'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> Parent, View view, int position, long id){
        Intent intent_list_click = new Intent(getBaseContext(), CeremonyDetailActivity.class);
        intent_list_click.putExtra("ceremony_name", listDataArray.get(position).getText_ceremony_name());
        intent_list_click.putExtra("ceremony_detail", listDataArray.get(position).getText_ceremony_detail());
        intent_list_click.putExtra("ceremony_sort", listDataArray.get(position).getText_ceremony_sort());
        intent_list_click.putExtra("ceremony_date", listDataArray.get(position).getText_ceremony_date());
        intent_list_click.putExtra("eid", listDataArray.get(position).getEID());

        startActivity(intent_list_click);
    }

    public class GetScheduleByEId extends AsyncTask<String, String, String> {
        int m_iEid;
        public GetScheduleByEId(int iEid)
        {
            m_iEid = iEid;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            BufferedInputStream bis = null;
            StringBuilder sb = new StringBuilder();
            try {
                URL url = new URL(GLOBALVAR.SCHEDULE_URL+ "/" + m_iEid);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                int responseCode;

                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);

                responseCode = con.getResponseCode();
                if (responseCode == 200) {
                    bis = new BufferedInputStream(con.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(bis, "UTF-8"));

                    String line = null;
                    while ((line = reader.readLine()) != null)
                        sb.append(line);
                    bis.close();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }

        protected void onProgressUpdate(String... progress) {

            //show 등록중입니다 프로세스
        }

        protected void onPostExecute(String result) {
            try{
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObj = (JSONObject) jsonParser.parse(result);
                String strTitle = jsonObj.get("title").toString();
                String strDate = jsonObj.get("date").toString();
                String strSort = jsonObj.get("sort").toString();

                String strEid = jsonObj.get("eid").toString();
                String strDetail = jsonObj.get("detail").toString();
                listDataArray.add(0, new ListData(strDate, strTitle, strSort, strDetail, Integer.parseInt(strEid)));
                iFinishItemCnt++;
                //when get all of the item
                if(iItemCnt == iFinishItemCnt)
                {
                    spinner.setVisibility(View.GONE);
                    onResume();
                }
            }
            catch(ParseException e)
            {
                e.printStackTrace();
            }
        }
    }


    public class GetEidByUid extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... args) {
            BufferedInputStream bis = null;
            StringBuilder sb = new StringBuilder();
            try {
                URL url = new URL(GLOBALVAR.RELATION_IDNAME_URL+ "/" + auth.getUserId());
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                int responseCode;

                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);

                responseCode = con.getResponseCode();
                if (responseCode == 200) {
                    bis = new BufferedInputStream(con.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(bis, "UTF-8"));

                    String line = null;
                    while ((line = reader.readLine()) != null)
                        sb.append(line);
                    bis.close();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return sb.toString();
        }

        protected void onProgressUpdate(String... progress) {

            //show 등록중입니다 프로세스
        }

        protected void onPostExecute(String result) {
            try{
                if(result.compareTo("[]") == 0)
                {
                    spinner.setVisibility(View.GONE);
                    onResume();
                }
                else
                {
                    String[] strArr = result.split(",");
                    //get eids
                    int[] iaEids = new int[strArr.length/4];
                    int iIndex =0;
                    for(int i=0; i<strArr.length; i++)
                    {
                        if(strArr[i].contains("eid"))
                        {
                            String strNum = strArr[i].replaceAll("\\D", "");
                            iaEids[iIndex] = Integer.parseInt(strNum);
                            new GetScheduleByEId(iaEids[iIndex]).execute();
                            iIndex++;
                            iItemCnt++;
                        }
                    }
                }
            }
            catch(Exception e)
            {

            }
        }
    }

}
