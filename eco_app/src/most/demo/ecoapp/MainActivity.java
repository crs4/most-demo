package most.demo.ecoapp;



import most.demo.ecoapp.config_fragments.ConfigFragment;
import most.demo.ecoapp.config_fragments.Fragment_EnterPasscode;
import most.demo.ecoapp.config_fragments.Fragment_PatientSelection;
import most.demo.ecoapp.config_fragments.Fragment_UserSelection;
import most.demo.ecoapp.config_fragments.LoginFragment;
import most.demo.ecoapp.models.EcoUser;
import most.demo.ecoapp.models.Patient;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements IConfigBuilder {
   
	private static String [] pages = { "User Selection",
									   "Pass Code",
									   "Emergency Patient Selection", 
									   "Task Group",
									   "Summary"};
	
	private ConfigFragment [] configFragments = null;

	private MostViewPager vpPager  = null;
	private EcoUser ecoUser = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupConfigFragments();
		setContentView(R.layout.activity_main);
		vpPager = (MostViewPager) findViewById(R.id.vpPager);
        adapterViewPager = new MyPagerAdapter(this,getSupportFragmentManager());
        vpPager.setAdapter(adapterViewPager);
        vpPager.setOnPageListener();

	}

	
    private void setupConfigFragments() {
			
	   this.configFragments = new ConfigFragment[this.pages.length];
	   
	   this.configFragments[0] = Fragment_UserSelection.newInstance(this,1, "User Selection");	
	   this.configFragments[1] = Fragment_EnterPasscode.newInstance(this, 2, "Enter passcode");
	   this.configFragments[2] = Fragment_PatientSelection.newInstance(this, 3, "Patient Selection");
	}


	private SmartFragmentStatePagerAdapter adapterViewPager;

    // Extend from SmartFragmentStatePagerAdapter now instead for more dynamic ViewPager items
    public static class MyPagerAdapter extends SmartFragmentStatePagerAdapter {
       
    	private MainActivity activity = null;
       
        public MyPagerAdapter(MainActivity activity,FragmentManager fragmentManager) {
            super(fragmentManager);
            this.activity = activity; 
        }
        
        

        // Returns total number of pages
        @Override
        public int getCount() {
            return pages.length;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
        	  Toast.makeText(this.activity, 
                      "getItem on position:::: " + position, Toast.LENGTH_SHORT).show();
              if (position>=0 && position < pages.length)
                return this.activity.configFragments[position];
              else
            	  return null;
            }
       

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return pages[position];
        }

    }

    @Override
	public void listEcoUsers() {
		this.vpPager.setInternalCurrentItem(0,0);
	}
    
	@Override
	public void setEcoUser(EcoUser user) {
		this.ecoUser = user;
		
		if (this.ecoUser!=null)
		{   
			this.vpPager.setInternalCurrentItem(1,0);
		}
	}


	@Override
	public EcoUser getEcoUser() {
		return this.ecoUser;
	}


	

	@Override
	public void listPatients() {
		this.vpPager.setInternalCurrentItem(2,0);
	}

	@Override
	public void setPatient(Patient selectedUser) {
		// TODO Auto-generated method stub
		
	}


	
}