package com.matburt.mobileorg.Gui.Capture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuInflater;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class EditActivity extends FragmentActivity {
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	public final static String ACTIONMODE_ADDCHILD = "add_child";


	private NodeWrapper node;
	private String actionMode;
	
	private OrgDatabase orgDB;
	private MobileOrgApplication appInst;
	
	private EditDetailsFragment detailsFragment;
	private EditPayloadFragment payloadFragment;
	private EditPayloadFragment rawPayloadFragment;
	private long node_id;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.edit);

		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra("actionMode");
		this.node_id = intent.getLongExtra("node_id", -1);

		this.appInst = (MobileOrgApplication) this.getApplication();
		this.orgDB = appInst.getDB();
		
		if (savedInstanceState != null) {
			this.detailsFragment = (EditDetailsFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditDetailsFragment.class.getName());
			this.payloadFragment = (EditPayloadFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditPayloadFragment.class.getName());
			this.rawPayloadFragment = (EditPayloadFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditPayloadFragment.class.getName() + "raw");
		}
		// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}
	
		init();
		
		setupActionbarTabs(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getSupportFragmentManager().putFragment(outState,
				EditPayloadFragment.class.getName(), payloadFragment);
		getSupportFragmentManager().putFragment(outState,
				EditPayloadFragment.class.getName() + "raw", rawPayloadFragment);
		getSupportFragmentManager().putFragment(outState,
				EditDetailsFragment.class.getName(), detailsFragment);
        outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
	
	private void init() {
		if (this.detailsFragment == null)
			this.detailsFragment = new EditDetailsFragment();
		if (this.payloadFragment == null)
			this.payloadFragment = new EditPayloadFragment();
		if (this.rawPayloadFragment == null)
			this.rawPayloadFragment = new EditPayloadFragment();
		
		String defaultTodo = PreferenceManager
				.getDefaultSharedPreferences(this).getString("defaultTodo", "");
		Intent intent = getIntent();
		
		if (this.actionMode == null) {
			String subject = intent
					.getStringExtra("android.intent.extra.SUBJECT");
			if(subject == null)
				subject = "";
			String text = intent.getStringExtra("android.intent.extra.TEXT");
			if(text == null)
				text = "";

			node = new NodeWrapper(null);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo, subject);
			this.payloadFragment.init(text, true);
			this.actionMode = ACTIONMODE_CREATE;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			node = new NodeWrapper(null);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init("", true);
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			long nodeId = intent.getLongExtra("node_id", 0);
			node = new NodeWrapper(appInst.getDB().getNode(nodeId));
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init(node.getCleanedPayload(orgDB), true);
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			node = new NodeWrapper(null);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init("", true);
		}

		this.rawPayloadFragment.init(node.getRawPayload(orgDB), false);
	}


	private void setupActionbarTabs(Bundle savedInstanceState) {
		ActionBar actionbar = getSupportActionBar();

		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		ActionBar.Tab detailsTab = actionbar.newTab().setText("Details");
		detailsTab.setTabListener(new TabListener(detailsFragment, "details"));
		actionbar.addTab(detailsTab);
	    
		ActionBar.Tab payloadTab = actionbar.newTab().setText("Payload");
		payloadTab.setTabListener(new TabListener(payloadFragment, "payload"));
		actionbar.addTab(payloadTab);

	    ActionBar.Tab rawPayloadTab = actionbar.newTab().setText("Raw Payload");
	    rawPayloadTab.setTabListener(new TabListener(rawPayloadFragment, "raw_payload"));
	    actionbar.addTab(rawPayloadTab);
	    
		if (savedInstanceState != null) {
            actionbar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
	}
	

	private class TabListener implements ActionBar.TabListener {
		Fragment fragment;

		public TabListener(Fragment fragment, String tag) {
			this.fragment = fragment;

			FragmentTransaction fragmentTransaction = getSupportFragmentManager()
					.beginTransaction();

			if (fragment != null && fragment.isAdded() == false) {
				fragmentTransaction.add(R.id.editnode_fragment_container,
						fragment, tag);
			}

			fragmentTransaction.hide(fragment).commit();
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {			
			FragmentTransaction fragmentTransaction = getSupportFragmentManager()
					.beginTransaction();
		    fragmentTransaction.show(fragment).commit();
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				FragmentTransaction fragmentTransaction = getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.hide(fragment).commit();
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	};
	
    @Override
	public boolean onCreateOptionsMenu(android.support.v4.view.Menu menu) {
    	super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.edit, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			doCancel();
			return true;
			
		case R.id.nodeedit_save:
			save();
			setResult(RESULT_OK);
			finish();
			return true;
			
		case R.id.nodeedit_cancel:
			doCancel();
			return true;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		doCancel();
	}
	
	private void doCancel() {
		if(this.detailsFragment.hasEdits() == false &&
                   this.payloadFragment.hasEdits() == false) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.node_edit_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResult(RESULT_CANCELED);
								finish();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}
	
	
	private void insertChangesIntoPayloadResidue() {
		node.getPayload(orgDB).insertOrReplace("SCHEDULED:", detailsFragment.getScheduled());
		node.getPayload(orgDB).insertOrReplace("DEADLINE:", detailsFragment.getDeadline());
	}
	
	private void save() {
		String newTitle = this.detailsFragment.getTitle();
		String newTodo = this.detailsFragment.getTodo();
		String newPriority = this.detailsFragment.getPriority();
		String newTags = this.detailsFragment.getTags();
		StringBuilder newCleanedPayload = new StringBuilder(this.payloadFragment.getText());
		insertChangesIntoPayloadResidue();
		String newPayloadResidue = node.getPayload(orgDB).getNewPayloadResidue();
						
		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
			OrgDatabase orgDB = appInst.getDB();
			long file_id = orgDB.addOrUpdateFile(OrgFile.CAPTURE_FILE, OrgFile.CAPTURE_FILE_ALIAS, "", true);
			Long parent = orgDB.getFileId(OrgFile.CAPTURE_FILE);
			long node_id = orgDB.addNode(parent, newTitle, newTodo, newPriority, newTags, file_id);
			
			boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("captureWithTimestamp", false);
			if(addTimestamp)
				newCleanedPayload.append("\n").append(getTimestamp()).append("\n");
			
			orgDB.addNodePayload(node_id, newCleanedPayload.toString() + newPayloadResidue);
			
			if(PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("calendarEnabled", false))
				appInst.getCalendarSyncService().insertNode(node_id);
			
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
			OrgDatabase orgDB = appInst.getDB();
			Long parent = this.node_id;
			long file_id = this.orgDB.getFileId(this.orgDB.getFilenameFromNodeId(parent));
			long node_id = orgDB.addNode(parent, newTitle, newTodo, newPriority, newTags, file_id);
			
			boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("captureWithTimestamp", false);
			if(addTimestamp)
				newCleanedPayload.append("\n").append(getTimestamp()).append("\n");
			
			orgDB.addNodePayload(node_id, newCleanedPayload.toString() + newPayloadResidue);
						
			makeNewheadingEditNode(node_id, new NodeWrapper(parent, orgDB));
			
			if(PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("calendarEnabled", false))
				appInst.getCalendarSyncService().insertNode(node_id);

		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			try {
				makeEditNodes(newTitle, newTodo, newPriority,
						newCleanedPayload.toString(), newTags);
			} catch (IOException e) {
			}
		}
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		intent.putExtra("showToast", false);
		sendBroadcast(intent);
	}
	
	private void makeNewheadingEditNode(long node_id, NodeWrapper parent) {
		boolean generateEdits = !parent.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE);
		if(generateEdits == false)
			return;

		// Add new heading nodes need the entire content of node without star headings
		String newContent = orgDB.nodeToString(node_id, 1).replaceFirst("[\\*]*", "");
		orgDB.addEdit("newheading", parent.getNodeId(orgDB), parent.getName(), "", newContent);
	}
	
	/**
	 * Takes a Node and five strings, representing edits to the node.
	 * This function will generate a new edit entry for each value that was 
	 * changed.
	 */ 
	private void makeEditNodes(String newTitle, String newTodo,
			String newPriority, String newCleanedPayload, String newTags) throws IOException {
		boolean generateEdits = !node.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE);
		
		if (!node.getName().equals(newTitle)) {
			if (generateEdits)
				orgDB.addEdit("heading", node.getNodeId(orgDB), newTitle, node.getName(), newTitle);
			node.setName(newTitle, orgDB);
		}
		if (newTodo != null && !node.getTodo().equals(newTodo)) {
			if (generateEdits)
				orgDB.addEdit("todo", node.getNodeId(orgDB), newTitle, node.getTodo(), newTodo);
			node.setTodo(newTodo, orgDB);
		}
		if (newPriority != null && !node.getPriority().equals(newPriority)) {
			if (generateEdits)
				orgDB.addEdit("priority", node.getNodeId(orgDB), newTitle, node.getPriority(),
					newPriority);
			node.setPriority(newPriority, orgDB);
		}
		if (!node.getCleanedPayload(orgDB).equals(newCleanedPayload)
				|| !node.getPayload(orgDB).getPayloadResidue()
						.equals(node.getPayload(orgDB).getNewPayloadResidue())) {
			String newRawPayload = node.getPayload(orgDB)
					.getNewPayloadResidue() + newCleanedPayload;

			if (generateEdits)
				orgDB.addEdit("body", node.getNodeId(orgDB), newTitle, node.getRawPayload(orgDB), newRawPayload);
			node.setPayload(newRawPayload, orgDB);
		}
		if(!node.getTags().equals(newTags)) {
			if (generateEdits) {
				orgDB.addEdit("tags", node.getNodeId(orgDB), newTitle,
						node.getTagsWithoutInheritet(),
						NodeWrapper.getTagsWithoutInheritet(newTags));
			}
			node.setTags(newTags, orgDB);
		}
	}
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
	
	@SuppressLint("NewApi")
	@Override
	public void finish() {
		super.finish();
		// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}	
	}
}
