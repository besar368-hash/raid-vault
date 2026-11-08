package com.raidvault.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Bg = Color(0xFF0B0D12)
private val Surface = Color(0xFF151923)
private val Surface2 = Color(0xFF1C2230)
private val Gold = Color(0xFFD7A84A)
private val GoldSoft = Color(0xFF9F7A35)
private val White = Color(0xFFF3F1EC)
private val Muted = Color(0xFF9299A8)

private data class VaultAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val level: String = "",
    val power: String = "",
    val accountType: String = "End Game",
    val mythicals: String = "0",
    val legendaries: String = "0",
    val voidShards: String = "0",
    val sacredShards: String = "0",
    val gems: String = "0",
    val silver: String = "0",
    val notes: String = "",
    val champions: List<String> = emptyList()
)

private val championCatalog = listOf(
    "Marius", "Mikage", "Hivelord", "Taras", "Marichka",
    "Gnut", "Wixwell", "Titus", "Shy'ek", "Kro'khad",
    "Ninja", "Rotos", "Siphi", "Trunda", "Armanz",
    "Arbiter", "Duchess Lilitu", "Kymar", "Cardiel",
    "Harima", "Siegfrund", "Lydia", "Mithrala", "Geomancer"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RaidVaultApp(this) }
    }
}

@Composable
private fun RaidVaultApp(context: Context) {
    var accounts by remember { mutableStateOf(loadAccounts(context)) }
    var selectedTab by remember { mutableStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VaultAccount?>(null) }
    var viewing by remember { mutableStateOf<VaultAccount?>(null) }
    var query by remember { mutableStateOf("") }

    fun save(next: List<VaultAccount>) {
        accounts = next
        persistAccounts(context, next)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Bg,
            background = Bg,
            onBackground = White,
            surface = Surface,
            onSurface = White,
            secondary = GoldSoft
        )
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF11151D),
                    contentColor = White
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("▦", fontSize = 20.sp) },
                        label = { Text("Vault") },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("♜", fontSize = 20.sp) },
                        label = { Text("Champions") },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("⚙", fontSize = 20.sp) },
                        label = { Text("Settings") },
                        colors = navColors()
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = {
                            editing = null
                            showEditor = true
                        },
                        containerColor = Gold,
                        contentColor = Bg
                    ) {
                        Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        ) { padding ->

            when (selectedTab) {
                0 -> VaultScreen(
                    accounts, query,
                    { query = it },
                    { viewing = it },
                    padding
                )

                1 -> ChampionsScreen(accounts, padding)

                else -> SettingsScreen(accounts.size, padding)
            }
        }

        if (showEditor) {
            AccountEditor(
                initial = editing,
                onDismiss = { showEditor = false },
                onSave = { account ->
                    val next =
                        if (editing == null) {
                            accounts + account
                        } else {
                            accounts.map {
                                if (it.id == account.id) account else it
                            }
                        }

                    save(next)
                    showEditor = false
                }
            )
        }

        viewing?.let { account ->
            AccountDetails(
                account,
                onDismiss = { viewing = null },
                onEdit = {
                    editing = account
                    viewing = null
                    showEditor = true
                },
                onDelete = {
                    save(accounts.filterNot { it.id == account.id })
                    viewing = null
                }
            )
        }
    }
}

@Composable
private fun navColors() =
    NavigationBarItemDefaults.colors(
        selectedIconColor = Gold,
        selectedTextColor = Gold,
        indicatorColor = Color(0xFF292316),
        unselectedIconColor = Muted,
        unselectedTextColor = Muted
    )

@Composable
private fun VaultScreen(
    accounts: List<VaultAccount>,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (VaultAccount) -> Unit,
    padding: PaddingValues
) {
    val filtered = accounts.filter {
        it.name.contains(query, true) ||
            it.champions.any { c -> c.contains(query, true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 22.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RAID VAULT",
                        color = Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )

                    Text(
                        "Your account archive",
                        color = White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    Modifier.size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "RV",
                        color = Gold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryTile(
                    "ACCOUNTS",
                    accounts.size.toString(),
                    Modifier.weight(1f)
                )

                SummaryTile(
                    "MYTHICALS",
                    accounts.sumOf {
                        it.mythicals.toIntOrNull() ?: 0
                    }.toString(),
                    Modifier.weight(1f)
                )

                SummaryTile(
                    "LEGENDARIES",
                    accounts.sumOf {
                        it.legendaries.toIntOrNull() ?: 0
                    }.toString(),
                    Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search accounts or champions",
                        color = Muted
                    )
                },
                leadingIcon = {
                    Text("⌕", color = Gold, fontSize = 23.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SAVED ACCOUNTS",
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${filtered.size} total",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Surface)
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("♜", color = Gold, fontSize = 32.sp)

                        Text(
                            if (accounts.isEmpty())
                                "Your vault is empty"
                            else
                                "No matching accounts",
                            color = White,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            if (accounts.isEmpty())
                                "Tap + to save your first Raid account."
                            else
                                "Try another search.",
                            color = Muted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { account ->
                AccountCard(account) { onOpen(account) }
            }
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(
                1.dp,
                Color(0xFF242B39),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )

        Spacer(Modifier.height(5.dp))

        Text(
            value,
            color = Gold,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AccountCard(
    account: VaultAccount,
    onClick: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(
                1.dp,
                Color(0xFF252C39),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2518)),
                contentAlignment = Alignment.Center
            ) {
                Text("♜", color = Gold, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    account.name,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "${account.accountType}  •  Level ${account.level.ifBlank { "—" }}",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Text("›", color = Gold, fontSize = 26.sp)
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MiniStat(
                "POWER",
                account.power.ifBlank { "—" },
                Modifier.weight(1f)
            )

            MiniStat(
                "MYTH",
                account.mythicals,
                Modifier.weight(1f)
            )

            MiniStat(
                "LEG",
                account.legendaries,
                Modifier.weight(1f)
            )
        }

        if (account.champions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))

            Text(
                account.champions.take(4).joinToString("  ·  ") +
                    if (account.champions.size > 4)
                        "  +${account.champions.size - 4}"
                    else "",
                color = GoldSoft,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.clip(RoundedCornerShape(9.dp))
            .background(Surface2)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                label,
                color = Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                value,
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ChampionsScreen(
    accounts: List<VaultAccount>,
    padding: PaddingValues
) {
    val counts = accounts.flatMap { it.champions }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }

    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "CHAMPION LIBRARY",
            color = Gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Text(
            "Your roster",
            color = White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Champions selected across your saved accounts.",
            color = Muted,
            fontSize = 13.sp
        )

        if (counts.isEmpty()) {
            EmptyPanel(
                "No champions added yet",
                "Add an account and select champions to build your roster."
            )
        } else {
            counts.forEach { (champion, count) ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2518)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", color = Gold)
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        champion,
                        color = White,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "${count} account${if (count == 1) "" else "s"}",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    accountCount: Int,
    padding: PaddingValues
) {
    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "PREFERENCES",
            color = Gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Text(
            "Settings",
            color = White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "LOCAL STORAGE",
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                "Your vault is stored on this device.",
                color = White,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "$accountCount saved account${if (accountCount == 1) "" else "s"} · No account or sign-in required.",
                color = Muted,
                fontSize = 12.sp
            )
        }

        Column(
            Modifier.fillMaxWidth()
     
