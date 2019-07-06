package project.android.nextcloud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;

public class Login extends AppCompatActivity {
Button login;
EditText serverurl,username,password;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private OwnCloudClient mClient;
    AESCrypt a=new AESCrypt();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sharedPreferences = this.getSharedPreferences("logininfo", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        String usernames = sharedPreferences.getString("username", "");
        if(!usernames.equals(""))
        {
            Intent myintent =new Intent(Login.this,MainActivity.class);
            startActivity(myintent);
        }
        serverurl=findViewById(R.id.serverurl);
        username=findViewById(R.id.email);
        password=findViewById(R.id.password);
        login=findViewById(R.id.email_sign_in_button);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String encrp= null;
                try {
                    encrp = a.encrypt(password.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(!username.getText().toString().equals("") || !password.getText().toString().equals("") || !serverurl.getText().toString().equals("")) {
                    editor.putString("username", username.getText().toString());
                    editor.putString("pass",encrp);
                    editor.putString("serverurl",serverurl.getText().toString());
                    editor.commit();
                    try
                    {
                        Intent myintent =new Intent(Login.this,MainActivity.class);
                        startActivity(myintent);
                }
                catch (Exception e)
                    {
                        Toast.makeText(Login.this, "Incorrect Info", Toast.LENGTH_SHORT).show();
                    }

                }

                          }
        });
    }
}
