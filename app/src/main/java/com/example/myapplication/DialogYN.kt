package com.example.myapplication


import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction


class YNDialogFragment() : DialogFragment() {
    var onYes : () -> Unit = {}
    var onNo : () -> Unit = {}
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        val view =  inflater.inflate(R.layout.dialog_yes_no, container, false)
        val btnOK : Button = view.findViewById(R.id.button_ok)
        val btnNO : Button = view.findViewById(R.id.button_no)
        btnOK.setOnClickListener {
            dismiss()
            onYes()
        }
        btnNO.setOnClickListener {
            dismiss()
            onNo()
        }
        return view
    }

    /** The system calls this only when creating the layout in a dialog. */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }


    fun show(supportFragmentManager: FragmentManager) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        // For a little polish, specify a transition animation
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        // To make it fullscreen, use the 'content' root view as the container
        // for the fragment, which is always the root view for the activity
        transaction
            .add(android.R.id.content, this)
            .addToBackStack(null)
            .commit()
    }

}
