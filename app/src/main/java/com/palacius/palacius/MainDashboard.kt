package com.palacius.palacius

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palacius.palacius.data.EgresoDao
import com.palacius.palacius.data.MenuDao
import com.palacius.palacius.data.OrdenDao
import com.palacius.palacius.data.OrdenRepository
import com.palacius.palacius.ui.theme.*

enum class PantallaActiva {
    POS,
    ORDENES_PENDIENTES,
    HISTORIAL,
    EGRESOS,
    ADMIN
}

@Composable
fun MainDashboard(
    ordenDao: OrdenDao,
    ordenRepository: OrdenRepository,
    egresoDao: EgresoDao,
    menuDao: MenuDao
) {
    var pantallaActual by remember {
        mutableStateOf(
            PantallaActiva.POS
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                PalaciusBackgroundDark
            )
    ) {
        /*
         * BARRA LATERAL
         */
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(110.dp)
                .background(
                    PalaciusSurfaceDark
                )
                .padding(
                    vertical = 16.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "P S",
                color =
                    PalaciusPrimaryMustard,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier.padding(
                        bottom = 20.dp
                    )
            )

            BotonMenu(
                icono = "🍔",
                texto = "Menú",
                activo =
                    pantallaActual ==
                            PantallaActiva.POS
            ) {
                pantallaActual =
                    PantallaActiva.POS
            }

            BotonMenu(
                icono = "📝",
                texto = "Activas",
                activo =
                    pantallaActual ==
                            PantallaActiva
                                .ORDENES_PENDIENTES
            ) {
                pantallaActual =
                    PantallaActiva
                        .ORDENES_PENDIENTES
            }

            BotonMenu(
                icono = "📖",
                texto = "Historial",
                activo =
                    pantallaActual ==
                            PantallaActiva.HISTORIAL
            ) {
                pantallaActual =
                    PantallaActiva.HISTORIAL
            }

            BotonMenu(
                icono = "💸",
                texto = "Egresos",
                activo =
                    pantallaActual ==
                            PantallaActiva.EGRESOS
            ) {
                pantallaActual =
                    PantallaActiva.EGRESOS
            }

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            BotonMenu(
                icono = "📊",
                texto = "Admin",
                activo =
                    pantallaActual ==
                            PantallaActiva.ADMIN
            ) {
                pantallaActual =
                    PantallaActiva.ADMIN
            }
        }

        /*
         * DIVISOR
         */
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(
                    PalaciusDivider
                )
        )

        /*
         * CONTENIDO PRINCIPAL
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (pantallaActual) {

                PantallaActiva.POS -> {
                    PantallaPOS(
                        ordenRepository =
                            ordenRepository,
                        menuDao = menuDao
                    )
                }

                PantallaActiva
                    .ORDENES_PENDIENTES -> {

                    /*
                     * La nueva PantallaOrdenes segura
                     * solamente necesita OrdenDao.
                     */
                    PantallaOrdenes(
                        ordenDao = ordenDao
                    )
                }

                PantallaActiva.HISTORIAL -> {
                    PantallaHistorial(
                        ordenDao = ordenDao,
                        egresoDao = egresoDao
                    )
                }

                PantallaActiva.EGRESOS -> {
                    PantallaEgresos(
                        egresoDao = egresoDao
                    )
                }

                PantallaActiva.ADMIN -> {
                    PantallaAdmin(
                        menuDao = menuDao
                    )
                }
            }
        }
    }
}

@Composable
fun BotonMenu(
    icono: String,
    texto: String,
    activo: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(85.dp)
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                if (activo) {
                    PalaciusBackgroundDark
                } else {
                    Color.Transparent
                }
            )
            .clickable {
                onClick()
            },
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = icono,
            fontSize = 26.sp
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text = texto,
            color =
                if (activo) {
                    PalaciusPrimaryMustard
                } else {
                    PalaciusTextLight
                },
            fontSize = 12.sp,
            fontWeight =
                if (activo) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}