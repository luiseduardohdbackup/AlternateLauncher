package com.an.alternatelauncher;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Window;

import com.an.alternatelauncher.c.Page;
import com.an.alternatelauncher.d.Dbh;

public class Launcher extends FragmentActivity {
//	private static final String TAG = Launcher.class.getSimpleName();
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.launcher);
     
        setupViewPager();
        
    }
	
	private void setupViewPager() {
		
        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        
        List<Page> pageFragments = new ArrayList<Page>();
        
        pageFragments.add((Page) AppListFragment.newInstance());
        pageFragments.add((Page) AppGridFragment.newInstance());
        
        new AppPagerAdapter(getSupportFragmentManager(), pager, pageFragments);
        
		pager.setCurrentItem(determinePageInView());
        
	}
	
	private int determinePageInView() {
		Dbh dbh = new Dbh(this);
		boolean emptyDb = dbh.isEmpty();
		dbh.close();
		return emptyDb ? 0 : 1;
	}
	
    private class AppPagerAdapter extends FragmentStatePagerAdapter implements OnPageChangeListener {
    	
    	private final List<Page> mPageFragments;
    	private final int PAGE_COUNT;
    	
    	private volatile int mLastPage;
    	private volatile int mCurrentPage;
    	
        public AppPagerAdapter(FragmentManager fm, ViewPager pager, List<Page> pageFragments) {
            super(fm);
            
            mPageFragments = pageFragments;
            PAGE_COUNT = mPageFragments.size();
            
            mCurrentPage = pager.getCurrentItem();
            mLastPage = mCurrentPage;
            
            pager.setAdapter(this);
            pager.setOnPageChangeListener(this);
            
        }
        
    	public void onPageSelected(int position) {
    		mLastPage = mCurrentPage;
    		mCurrentPage = position;
    		pageTransition(mLastPage, mCurrentPage);
		}
		public void onPageScrollStateChanged(int state) {}
		public void onPageScrolled(int position, float offset, int offsetPixels) {}
		
        private void pageTransition(int oldPage, int newPage) {
        	
//        	Debug.d(TAG, "Transition from", oldPage, "to", newPage);
//        	
        	Page pageOld = mPageFragments.get(oldPage);
        	Page pageNew = mPageFragments.get(newPage);
        	
        	pageOld.pageOut(newPage > oldPage ? Page.RIGHT : Page.LEFT);
        	pageNew.pageIn(newPage > oldPage ? Page.LEFT : Page.RIGHT);
    		
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
        	return (Fragment) mPageFragments.get(position);
        }
        
    }
    
}
