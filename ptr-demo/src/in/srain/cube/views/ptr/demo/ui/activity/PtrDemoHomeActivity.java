package in.srain.cube.views.ptr.demo.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import in.srain.cube.mints.base.MintsBaseActivity;
import in.srain.cube.views.ptr.demo.R;
import in.srain.cube.views.ptr.demo.ui.MaterialStyleFragment;
import in.srain.cube.views.ptr.demo.ui.PtrDemoHomeFragment;

public class PtrDemoHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction().replace(R.id.id_fragment, new MaterialStyleFragment()).commitNow();
    }
}