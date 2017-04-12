package com.example;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements
		View.OnClickListener {

	private EditText etAdbPort;
	private Button btnRun;
	private TextView tvAdbOrder;

	private DataOutputStream dataOutputStream;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		etAdbPort = (EditText) findViewById(R.id.et_adb_port);
		btnRun = (Button) findViewById(R.id.btn_run);
		tvAdbOrder = (TextView) findViewById(R.id.tv_adb_order);

		btnRun.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.btn_run:
			setLinkAdbPort();
			break;

		default:
			break;
		}
	}

	/**
	 * 通过adb命令，设置WiFi调试真机的端口
	 */
	private void setLinkAdbPort() {
		String portString = etAdbPort.getText().toString().trim();

		if (TextUtils.isEmpty(portString)) {
			portString = "5555";
		}

		execShellCmd("stop adbd\n");
		execShellCmd("setprop service.adb.tcp.port " + portString + "\n");
		execShellCmd("start adbd\n");

		Toast.makeText(getApplicationContext(),
				getResources().getString(R.string.success), Toast.LENGTH_LONG)
				.show();
		showOrder(portString);
	}

	private void showOrder(String port) {

		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String orderString = String.format(
				getResources().getString(R.string.adb_order), "adb connect "
						+ Formatter.formatIpAddress(ipAddress)+":"+port);
		tvAdbOrder.setText(orderString);
		tvAdbOrder.setVisibility(View.VISIBLE);
	}

	private void execShellCmd(String cmd) {

		try {
			if (dataOutputStream == null) {

				// 申请获取root权限，这一步很重要，不然会没有作用
				Process process = Runtime.getRuntime().exec("su");
				// 获取输出流
				OutputStream outputStream = process.getOutputStream();
				dataOutputStream = new DataOutputStream(outputStream);
			}
			dataOutputStream.writeBytes(cmd);
			dataOutputStream.flush();
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	/*
	 * args[0] : shell 命令 如"ls" 或"ls -1"; args[1] : 命令执行路径 如"/" ;
	 */
	public String execute(String cmmand, String directory) throws IOException {
		String result = "";
		try {
			ProcessBuilder builder = new ProcessBuilder(cmmand);

			if (directory != null)
				builder.directory(new File(directory));
			builder.redirectErrorStream(true);
			Process process = builder.start();

			// 得到命令执行后的结果
			InputStream is = process.getInputStream();
			byte[] buffer = new byte[1024];
			while (is.read(buffer) != -1) {
				result = result + new String(buffer);
			}
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG)
				.show();

		return result;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (dataOutputStream != null) {

			try {
				dataOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			dataOutputStream = null;
		}
	}

}
