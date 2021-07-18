package wtf.s1.android.ptr.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SimpleListCompose() {
    LazyColumn {
        items(30) {index ->
            Row(Modifier.padding(16.dp)) {
//                Image(
//
//                    painter = rememberImagePainter(randomSampleImageUrl(index)),
//                    contentDescription = null,
//                    modifier = Modifier.size(64.dp),
//                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Text",
                    style = MaterialTheme.typography.subtitle2,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }

}