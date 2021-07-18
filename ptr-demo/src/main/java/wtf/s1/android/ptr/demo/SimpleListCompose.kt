package wtf.s1.android.ptr.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun SimpleListCompose(modifier: Modifier) {
    LazyColumn(modifier = modifier) {
        items(DotaList.size) { index ->
            Row(Modifier.padding(16.dp)) {
                DotaList[index].let {
                    Image(
                        painter = painterResource(id = it.avatar),
                        contentDescription = null,
                        modifier = Modifier
                            .width(80.dp)
                            .offset(12.dp, 8.dp)
                            .aspectRatio(256f / 144)
                            .clip(shape = RoundedCornerShape(3.dp))
                    )
                    Spacer(Modifier.width(8.dp))

                    Column(
                        Modifier
                            .offset(12.dp, 8.dp)
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            text = stringResource(id = it.name),
                            style = MaterialTheme.typography.subtitle1,
                        )

                        Text(
                            text = stringResource(id = it.text),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.offset(y = 8.dp)
                        )
                    }
                }
            }
        }
    }

}