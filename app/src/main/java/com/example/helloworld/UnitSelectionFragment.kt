package com.example.helloworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

/**
 *  Created by paulbisioc on 05.01.2022
 */

class UnitSelectionFragment : Fragment() {
    lateinit var MCUButton: Button
    lateinit var SFUButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val startingView = inflater.inflate(R.layout.fragment_unit_selection, container, false)
        MCUButton = startingView.findViewById(R.id.mcuButton)
        SFUButton = startingView.findViewById(R.id.sfuButton)
        return startingView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setControls()
    }

    private fun setControls() {
        SFUButton.setOnClickListener {
            (activity as MainActivity).let {
                it.isSFUSelected = true
                it.addDefaultVideoButtons()
            }
        }

        MCUButton.setOnClickListener {
            (activity as MainActivity).let {
                it.isSFUSelected = false
                it.addDefaultVideoButtons()
            }
        }
    }

    companion object {
        fun newInstance(): UnitSelectionFragment {
            val fragment = UnitSelectionFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }
}