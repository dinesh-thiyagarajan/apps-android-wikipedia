package org.wikipedia.analytics.eventplatform

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.ancestors
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.feed.view.ListCardView
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.navtab.NavTab
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.onboarding.InitialOnboardingFragment
import org.wikipedia.onboarding.InitialOnboardingFragment.OnboardingPage
import org.wikipedia.page.ExclusiveBottomSheetPresenter

object BreadCrumbViewUtil {
    private const val VIEW_UNNAMED = "unnamed"

    fun getReadableNameForView(view: View): String {
        val parent = view.parent
        if (parent is RecyclerView) {
            val position = parent.findContainingViewHolder(view)?.bindingAdapterPosition ?: 0
            val parentCardName = if (view is ListCardItemView) {
                // If null, ListItemView is not in a CardView
                view.ancestors.firstOrNull { it is ListCardView<*> }?.javaClass?.simpleName
            } else {
                null
            }
            // If no parent card is available, return only recyclerview name and click position for
            // non-cardView recyclerViews
            val parentName = parentCardName ?: getReadableNameForView(parent)
            return "$parentName.$position"
        }
        return if (view.id == View.NO_ID) {
            if (view is TextView && view !is EditText) {
                view.text.toString()
            } else {
                VIEW_UNNAMED
            }
        } else {
            getViewResourceName(view)
        }
    }

    private fun getViewResourceName(view: View): String {
        return try {
            if (view.id == R.id.footerActionButton) {
                return (view as MaterialButton).text.toString()
            }
            view.resources.getResourceEntryName(view.id)
        } catch (e: Exception) {
            VIEW_UNNAMED
        }
    }

    fun getReadableScreenName(context: Context, fragment: Fragment? = null): String {
        val fragName = if (fragment == null) getCurrentFragmentName(context) else fragment.javaClass.simpleName
        return context.javaClass.simpleName + (if (fragName.isNotEmpty()) ".$fragName" else "")
    }

    private fun getCurrentFragmentName(context: Context): String {
        val fragment = getVisibleFragment(context)

        return when {
            fragment != null && context is InitialOnboardingActivity -> {
                getInitialOnboardingScreenName(fragment)
            }
            fragment != null && context is MainActivity -> {
                getMainFragmentTabName(fragment)
            }
            fragment != null -> {
                return fragment.javaClass.simpleName
            }
            else -> return ""
        }
    }

    private fun getMainFragmentTabName(fragment: Fragment): String {
        if (fragment is MainFragment) {
            val mainFragmentPager: ViewPager2? = fragment.view?.findViewById(R.id.main_view_pager)
            return if (mainFragmentPager == null) {
                fragment.javaClass.simpleName
            } else {
                NavTab.of(mainFragmentPager.currentItem).name
            }
        }
        return ""
    }

    private fun getInitialOnboardingScreenName(fragment: Fragment): String {
        if (fragment is InitialOnboardingFragment) {
            val onboardingFragmentPager: ViewPager2? =
                fragment.view?.findViewById(R.id.fragment_pager)
            return if (onboardingFragmentPager == null) {
                fragment.javaClass.simpleName
            } else {
                OnboardingPage.of(onboardingFragmentPager.currentItem).name
            }
        }
        return ""
    }

    private fun getVisibleFragment(context: Context): Fragment? {
        if (context is SingleFragmentActivity<*>) {
            return context.supportFragmentManager.findFragmentById(R.id.fragment_container)
        } else if (context is FragmentActivity) {
            val fragments: List<Fragment> = context.supportFragmentManager.fragments
            for (fragment in fragments) {
                if (fragment.isVisible) return fragment
            }
        } else if (context is ContextThemeWrapper && context.baseContext is FragmentActivity) {
            // Very likely a bottom sheet, so find it within the Context's fragment structure.
            var targetFrag = (context.baseContext as FragmentActivity).supportFragmentManager.findFragmentByTag(ExclusiveBottomSheetPresenter.BOTTOM_SHEET_FRAGMENT_TAG)
            if (targetFrag != null) {
                return targetFrag
            }
            val frags = (context.baseContext as FragmentActivity).supportFragmentManager.fragments
            frags.forEach {
                targetFrag = it.childFragmentManager.findFragmentByTag(ExclusiveBottomSheetPresenter.BOTTOM_SHEET_FRAGMENT_TAG)
                if (targetFrag != null) {
                    return targetFrag
                }
            }
            frags.lastOrNull()
        }
        return null
    }
}
