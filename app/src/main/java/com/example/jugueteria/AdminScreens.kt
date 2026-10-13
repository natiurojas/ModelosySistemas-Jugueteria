package com.example.jugueteria

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private fun auth(activity: AppCompatActivity) = "Token " + (activity.getSharedPreferences("jugueteria", 0).getString("token", "") ?: "")
private fun <T> Call<T>.watch(activity: AppCompatActivity, done: (Response<T>?, Throwable?) -> Unit) = enqueue(object: Callback<T> {
    override fun onResponse(call: Call<T>, response: Response<T>) = done(response, null)
    override fun onFailure(call: Call<T>, t: Throwable) = done(null, t)
})
private data class AdminRow(val id: Int, val title: String, val subtitle: String, val extra: String)
private class AdminRowAdapter(private val rows: List<AdminRow>, private val edit: (AdminRow)->Unit, private val delete: (AdminRow)->Unit): RecyclerView.Adapter<AdminRowAdapter.Holder>() {
    class Holder(v: View): RecyclerView.ViewHolder(v) { val title: TextView=v.findViewById(R.id.txtRecordTitle); val subtitle: TextView=v.findViewById(R.id.txtRecordSubtitle); val extra: TextView=v.findViewById(R.id.txtRecordExtra); val edit: ImageButton=v.findViewById(R.id.btnEditRecord); val delete: ImageButton=v.findViewById(R.id.btnDeleteRecord) }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_record,parent,false))
    override fun getItemCount() = rows.size
    override fun onBindViewHolder(h: Holder, p: Int) { val r=rows[p]; h.title.text=r.title; h.subtitle.text=r.subtitle; h.extra.text=r.extra; h.edit.setOnClickListener{edit(r)}; h.delete.setOnClickListener{delete(r)} }
}

class UsersActivity: AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); ApiClient.restore(this); setContentView(R.layout.activity_admin_list); findViewById<TextView>(R.id.txtScreenTitle).text="Usuarios"; findViewById<Button>(R.id.btnCreate).text="+ Crear Usuarios"; findViewById<ImageButton>(R.id.btnBack).setOnClickListener{finish()}; findViewById<Button>(R.id.btnCreate).setOnClickListener{startActivity(Intent(this,UserEditActivity::class.java))} }
 override fun onResume(){super.onResume(); load()}
 private fun load(){ val empty=findViewById<TextView>(R.id.txtEmpty); ApiClient.api.users(auth(this)).watch(this){r,e-> if(e!=null||r==null||!r.isSuccessful){empty.text="No se pudieron cargar los usuarios. Verificá la conexión y los permisos de administrador.";return@watch}; val rows=r.body().orEmpty().map{AdminRow(it.id,it.username,it.email,"Rol: ${it.role}")}; empty.text=if(rows.isEmpty())"No hay usuarios registrados." else "Usuarios disponibles: ${rows.size}"; findViewById<RecyclerView>(R.id.recyclerAdmin).apply{layoutManager=LinearLayoutManager(this@UsersActivity);adapter=AdminRowAdapter(rows,{row->startActivity(Intent(this@UsersActivity,UserEditActivity::class.java).putExtra("id",row.id))},{row->confirmDelete("¿Eliminar al usuario ${row.title}?"){ApiClient.api.deleteUser(auth(this@UsersActivity),row.id).watch(this@UsersActivity){res,err->Toast.makeText(this@UsersActivity,if(err==null&&res?.isSuccessful==true)"Usuario eliminado" else "No se pudo eliminar el usuario",Toast.LENGTH_LONG).show();load()}}})} } }
 private fun confirmDelete(message:String, action:()->Unit){AlertDialog.Builder(this).setMessage(message).setNegativeButton("Cancelar",null).setPositiveButton("Eliminar"){_,_->action()}.show()}
}

class UserEditActivity: AppCompatActivity(){
 private var userId:Int?=null
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);ApiClient.restore(this);setContentView(R.layout.activity_user_edit);userId=intent.getIntExtra("id",-1).takeIf{it>0};findViewById<ImageButton>(R.id.btnBack).setOnClickListener{finish()};val name=findViewById<EditText>(R.id.edtUserName);val email=findViewById<EditText>(R.id.edtUserEmail);val pass=findViewById<EditText>(R.id.edtUserPassword);val role=findViewById<Spinner>(R.id.spnUserRole);role.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("administrador","gerente","deposito","cajero"));findViewById<TextView>(R.id.txtFormTitle).text=if(userId==null)"Crear usuario" else "Editar usuario"; if(userId!=null){pass.isEnabled=false;pass.hint="La contraseña no se puede editar aquí";ApiClient.api.users(auth(this)).watch(this){r,_->val u=r?.body()?.firstOrNull{it.id==userId};if(u!=null){name.setText(u.username);email.setText(u.email);role.setSelection(listOf("administrador","gerente","deposito","cajero").indexOf(u.role).coerceAtLeast(0))}}};findViewById<Button>(R.id.btnSaveForm).setOnClickListener{val n=name.text.toString().trim();val em=email.text.toString().trim();if(n.isBlank()||em.isBlank()||(userId==null&&pass.text.isBlank())){Toast.makeText(this,"Completá nombre, correo y contraseña para usuarios nuevos",Toast.LENGTH_LONG).show();return@setOnClickListener};val data= mutableMapOf<String,Any>("username" to n,"email" to em,"role" to role.selectedItem.toString());val call=if(userId==null){data["password"]=pass.text.toString();ApiClient.api.createUser(auth(this),AdminUserRequest(n,em,pass.text.toString(),role.selectedItem.toString()))}else ApiClient.api.updateUser(auth(this),userId!!,data);call.watch(this){r,e->if(e==null&&r?.isSuccessful==true){Toast.makeText(this,"Usuario guardado",Toast.LENGTH_SHORT).show();finish()}else Toast.makeText(this,"No se pudo guardar el usuario (${r?.code() ?: e?.message})",Toast.LENGTH_LONG).show()}} }
}

class OfficeActivity: AppCompatActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);ApiClient.restore(this);setContentView(R.layout.activity_admin_list);findViewById<TextView>(R.id.txtScreenTitle).text="Sucursales";findViewById<Button>(R.id.btnCreate).text="+ Crear Sucursal";findViewById<ImageButton>(R.id.btnBack).setOnClickListener{finish()};findViewById<Button>(R.id.btnCreate).setOnClickListener{startActivity(Intent(this,OfficeEditActivity::class.java))} }
 override fun onResume(){super.onResume();load()}
 private fun load(){val empty=findViewById<TextView>(R.id.txtEmpty);ApiClient.api.offices(auth(this)).watch(this){r,e->if(e!=null||r==null||!r.isSuccessful){empty.text="No se pudieron cargar las sucursales. Verificá los permisos y la conexión.";return@watch};val rows=r.body().orEmpty().map{AdminRow(it.id,it.nombre,it.domicilio,"Razón social: ${it.razon_social}")};empty.text=if(rows.isEmpty())"No hay sucursales registradas." else "Sucursales disponibles: ${rows.size}";findViewById<RecyclerView>(R.id.recyclerAdmin).apply{layoutManager=LinearLayoutManager(this@OfficeActivity);adapter=AdminRowAdapter(rows,{startActivity(Intent(this@OfficeActivity,OfficeEditActivity::class.java).putExtra("id",it.id))},{row->AlertDialog.Builder(this@OfficeActivity).setMessage("¿Eliminar la sucursal ${row.title}?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar"){_,_->ApiClient.api.deleteOffice(auth(this@OfficeActivity),row.id).watch(this@OfficeActivity){res,err->Toast.makeText(this@OfficeActivity,if(err==null&&res?.isSuccessful==true)"Sucursal eliminada" else "No se pudo eliminar (puede tener cajas asociadas)",Toast.LENGTH_LONG).show();load()}}.show()})}} }
}
class OfficeEditActivity:AppCompatActivity(){private var id:Int?=null;override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);ApiClient.restore(this);setContentView(R.layout.activity_office_edit);id=intent.getIntExtra("id",-1).takeIf{it>0};findViewById<ImageButton>(R.id.btnBack).setOnClickListener{finish()};val n=findViewById<EditText>(R.id.edtOfficeName);val d=findViewById<EditText>(R.id.edtOfficeAddress);val rs=findViewById<EditText>(R.id.edtOfficeCompany);findViewById<TextView>(R.id.txtFormTitle).text=if(id==null)"Crear sucursal" else "Editar sucursal";if(id!=null)ApiClient.api.offices(auth(this)).watch(this){r,_->r?.body()?.firstOrNull{it.id==id}?.let{n.setText(it.nombre);d.setText(it.domicilio);rs.setText(it.razon_social)}};findViewById<Button>(R.id.btnSaveForm).setOnClickListener{if(n.text.isBlank()||d.text.isBlank()||rs.text.isBlank()){Toast.makeText(this,"Completá todos los campos",Toast.LENGTH_SHORT).show();return@setOnClickListener};val req=BranchRequest(n.text.toString(),d.text.toString(),rs.text.toString());val call=if(id==null)ApiClient.api.createBranch(auth(this),req) else ApiClient.api.updateOffice(auth(this),id!!,req);call.watch(this){r,e->if(e==null&&r?.isSuccessful==true){Toast.makeText(this,"Sucursal guardada",Toast.LENGTH_SHORT).show();finish()}else Toast.makeText(this,"No se pudo guardar la sucursal (${r?.code() ?: e?.message})",Toast.LENGTH_LONG).show()}}}}

class CashboxActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState:Bundle?)
    {
        super.onCreate(savedInstanceState);
        ApiClient.restore(this);
        setContentView(R.layout.activity_admin_list);
        findViewById<TextView>(R.id.txtScreenTitle).text="Cajas";
        findViewById<Button>(R.id.btnCreate).text="+ Crear Caja";
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener{
            finish()
        };
        findViewById<Button>(R.id.btnCreate).setOnClickListener{
            startActivity(Intent(this,CashboxEditActivity::class.java))
        }
    }
    override fun onResume()
    {
        super.onResume();
        load()
    };
    private fun load()
    {
        val empty=findViewById<TextView>(R.id.txtEmpty);
        ApiClient.api.cashboxes(auth(this)).watch(this){r, e ->
            if(e!=null||r==null||!r.isSuccessful)
            {
                empty.text="No se pudieron cargar las cajas. Verificá permisos y conexión.";
                Log.e("CashboxActivity", r.toString())
                return@watch
            }
            val rows = r.body().orEmpty().map{AdminRow(it.id,"Caja #${it.id} · ${it.nombre}","Sucursal ID: ${it.sucursal}","Cajero ID: ${it.cajero ?: "Sin asignar"}${it.cajero_nombre?.let{" ($it)"}?:""} · ${it.hora_apertura}–${it.hora_cierre}")};empty.text=if(rows.isEmpty())"No hay cajas registradas." else "Cajas disponibles: ${rows.size}";findViewById<RecyclerView>(R.id.recyclerAdmin).apply{layoutManager=LinearLayoutManager(this@CashboxActivity);adapter=AdminRowAdapter(rows,{startActivity(Intent(this@CashboxActivity,CashboxEditActivity::class.java).putExtra("id",it.id))},{row->AlertDialog.Builder(this@CashboxActivity).setMessage("¿Eliminar la caja ${row.title}?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar"){_,_->ApiClient.api.deleteCashbox(auth(this@CashboxActivity),row.id).watch(this@CashboxActivity){res,err->Toast.makeText(this@CashboxActivity,if(err==null&&res?.isSuccessful==true)"Caja eliminada" else "No se pudo eliminar la caja",Toast.LENGTH_LONG).show();load()}}.show()})}}}}
class CashboxEditActivity:AppCompatActivity(){private var id:Int?=null;override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);ApiClient.restore(this);setContentView(R.layout.activity_cashbox_edit);id=intent.getIntExtra("id",-1).takeIf{it>0};findViewById<ImageButton>(R.id.btnBack).setOnClickListener{finish()};val branch=findViewById<EditText>(R.id.edtCashOffice);val name=findViewById<EditText>(R.id.edtCashName);val cashier=findViewById<EditText>(R.id.edtCashier);val open=findViewById<EditText>(R.id.edtCashOpen);val close=findViewById<EditText>(R.id.edtCashClose);findViewById<TextView>(R.id.txtFormTitle).text=if(id==null)"Crear caja" else "Editar caja";if(id!=null)ApiClient.api.cashboxes(auth(this)).watch(this){r,_->r?.body()?.firstOrNull{it.id==id}?.let{branch.setText(it.sucursal.toString());name.setText(it.nombre);cashier.setText(it.cajero?.toString().orEmpty());open.setText(it.hora_apertura.take(5));close.setText(it.hora_cierre.take(5))}};findViewById<Button>(R.id.btnSaveForm).setOnClickListener{val b=branch.text.toString().toIntOrNull();if(b==null||name.text.isBlank()){Toast.makeText(this,"Ingresá una sucursal válida y el nombre de la caja",Toast.LENGTH_SHORT).show();return@setOnClickListener};val req=CashRegisterRequest(b,name.text.toString(),cashier.text.toString().toIntOrNull(),open.text.toString(),close.text.toString());val call=if(id==null)ApiClient.api.createCashRegister(auth(this),req)else ApiClient.api.updateCashbox(auth(this),id!!,req);call.watch(this){r,e->if(e==null&&r?.isSuccessful==true){Toast.makeText(this,"Caja guardada",Toast.LENGTH_SHORT).show();finish()}else Toast.makeText(this,"No se pudo guardar la caja (${r?.code() ?: e?.message})",Toast.LENGTH_LONG).show()}}}}
