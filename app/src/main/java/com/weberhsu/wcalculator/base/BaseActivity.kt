package com.weberhsu.wcalculator.base

import android.os.*
import android.view.*
import androidx.appcompat.app.*
import androidx.viewbinding.*
import com.gyf.immersionbar.*
import com.weberhsu.wcalculator.databinding.*

/**
 * author : weber
 * e-mail : weber0207@gmail.com
 * time : 2023/09/13 12:00 AM * version: 1.0
 * desc : Base of activity
 */
abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = createBinding(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).init()
    }

    abstract fun createBinding(layoutInflater: LayoutInflater): VB
}