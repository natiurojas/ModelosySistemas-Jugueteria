package com.jugueteria.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------- Colores del tema (juguetería, alegre y colorido) ----------
private val NaranjaJuguete = Color(0xFFFF7A29)
private val AmarilloJuguete = Color(0xFFFFC93C)
private val AzulJuguete = Color(0xFF2E86FF)
private val FondoSuave = Color(0xFFFFF8F0)
private val RojoError = Color(0xFFE53935)

private val categorias = listOf(
    "Muñecas", "Bloques y construcción", "Peluches",
    "Juegos de mesa", "Vehículos", "Aire libre", "Otros"
)
private val edadesRecomendadas = listOf("0-2 años", "3-5 años", "6-8 años", "9-12 años", "13+ años")

// ---------- Estado del formulario ----------
data class ProductoFormState(
    val nombre: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val stock: String = "",
    val categoria: String = "",
    val edad: String = "",
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorStock: String? = null,
    val errorCategoria: String? = null,
    val errorEdad: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormScreen(onGuardar: (ProductoFormState) -> Unit = {}) {
    var estado by remember { mutableStateOf(ProductoFormState()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = FondoSuave,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧸", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargar producto", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NaranjaJuguete)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {

            Text(
                "Juguetería Arcoíris",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NaranjaJuguete
            )
            Text(
                "Completá los datos del nuevo producto",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Selector de imagen (placeholder visual, conectalo con tu galería/cámara)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmarilloJuguete.copy(alpha = 0.25f))
                    .clickable {
                        scope.launch {
                            snackbarHostState.showSnackbar("Acá se abriría la galería o cámara")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Agregar foto del juguete", color = NaranjaJuguete, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nombre
            OutlinedTextField(
                value = estado.nombre,
                onValueChange = { estado = estado.copy(nombre = it, errorNombre = null) },
                label = { Text("Nombre del producto") },
                singleLine = true,
                isError = estado.errorNombre != null,
                supportingText = { estado.errorNombre?.let { Text(it, color = RojoError) } },
                modifier = Modifier.fillMaxWidth(),
                colors = camposColores()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Descripción (opcional, sin validación)
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = { estado = estado.copy(descripcion = it) },
                label = { Text("Descripción (opcional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = camposColores()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Precio
                OutlinedTextField(
                    value = estado.precio,
                    onValueChange = { estado = estado.copy(precio = it, errorPrecio = null) },
                    label = { Text("Precio ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = estado.errorPrecio != null,
                    supportingText = { estado.errorPrecio?.let { Text(it, color = RojoError) } },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = camposColores()
                )

                // Stock
                OutlinedTextField(
                    value = estado.stock,
                    onValueChange = { estado = estado.copy(stock = it, errorStock = null) },
                    label = { Text("Stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = estado.errorStock != null,
                    supportingText = { estado.errorStock?.let { Text(it, color = RojoError) } },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = camposColores()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Categoría (desplegable)
            DropdownSeleccionable(
                etiqueta = "Categoría",
                opciones = categorias,
                seleccionado = estado.categoria,
                error = estado.errorCategoria,
                onSeleccionar = { estado = estado.copy(categoria = it, errorCategoria = null) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Edad recomendada (desplegable)
            DropdownSeleccionable(
                etiqueta = "Edad recomendada",
                opciones = edadesRecomendadas,
                seleccionado = estado.edad,
                error = estado.errorEdad,
                onSeleccionar = { estado = estado.copy(edad = it, errorEdad = null) }
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { estado = ProductoFormState() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        val nuevoEstado = validarFormulario(estado)
                        estado = nuevoEstado
                        val huboError = listOf(
                            nuevoEstado.errorNombre,
                            nuevoEstado.errorPrecio,
                            nuevoEstado.errorStock,
                            nuevoEstado.errorCategoria,
                            nuevoEstado.errorEdad
                        ).any { it != null }

                        if (!huboError) {
                            onGuardar(nuevoEstado)
                            scope.launch {
                                snackbarHostState.showSnackbar("¡Producto agregado correctamente! 🧸")
                            }
                            estado = ProductoFormState()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Revisá los campos marcados en rojo")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaJuguete)
                ) {
                    Text("Guardar producto", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun camposColores() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AzulJuguete,
    unfocusedBorderColor = Color.LightGray,
    errorBorderColor = RojoError,
    focusedLabelColor = AzulJuguete
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSeleccionable(
    etiqueta: String,
    opciones: List<String>,
    seleccionado: String,
    error: String?,
    onSeleccionar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido }
    ) {
        OutlinedTextField(
            value = seleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            isError = error != null,
            supportingText = { error?.let { Text(it, color = RojoError) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = camposColores()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

// ---------- Validaciones ----------
private fun validarFormulario(estado: ProductoFormState): ProductoFormState {
    val errorNombre = if (estado.nombre.isBlank()) "Ingresá el nombre del producto" else null

    val precioNum = estado.precio.toDoubleOrNull()
    val errorPrecio = when {
        estado.precio.isBlank() -> "Ingresá un precio"
        precioNum == null -> "El precio debe ser un número"
        precioNum <= 0 -> "El precio debe ser mayor a 0"
        else -> null
    }

    val stockNum = estado.stock.toIntOrNull()
    val errorStock = when {
        estado.stock.isBlank() -> "Ingresá el stock disponible"
        stockNum == null -> "El stock debe ser un número entero"
        stockNum < 0 -> "El stock no puede ser negativo"
        else -> null
    }

    val errorCategoria = if (estado.categoria.isBlank()) "Seleccioná una categoría" else null
    val errorEdad = if (estado.edad.isBlank()) "Seleccioná una edad recomendada" else null

    return estado.copy(
        errorNombre = errorNombre,
        errorPrecio = errorPrecio,
        errorStock = errorStock,
        errorCategoria = errorCategoria,
        errorEdad = errorEdad
    )
}

@Preview(showBackground = true)
@Composable
private fun ProductoFormScreenPreview() {
    MaterialTheme {
        ProductoFormScreen()
    }
}
