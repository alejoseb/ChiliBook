package com.alejoseb.chillibook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import com.alejoseb.chillibook.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    private  var blename: String = "empty"
    private  var bleaddress: String = "empty"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }



    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?  ): View? {


        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        val bundle = arguments
        if(bundle != null) {
            if (bundle!!.containsKey("blename")) {
                blename =  bundle!!.getString("blename").toString()
            }
            if (bundle!!.containsKey("bleaddress")) {
                bleaddress =  bundle!!.getString("bleaddress").toString()
            }
        }



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        var textViewsecond =  view.findViewById<TextView>(R.id.textview_second)
        textViewsecond.setText( String.format("Name: %s, Address: %s ",blename , bleaddress ))

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}