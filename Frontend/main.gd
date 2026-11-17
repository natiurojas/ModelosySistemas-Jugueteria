@tool
extends ControlCSS
@onready var User: LineEditCSS = %User
@onready var Password: LineEditCSS = %Password
@onready var ViewPasswordBtn: TextureButtonCSS = %ViewPasswordBtn
@onready var ErrorTxt: LabelCSS = %ErrorTxt
@onready var LoginBtn: ButtonCSS = %LoginBtn
@onready var LoadingCircle: TextureRectCSS = %LoadingCircle
@onready var LoadingCircleAnim: AnimationPlayer = %LoadingCircleAnim

const textures_password = {
	"open_eye": preload("uid://bd4x67vv4noo7"),
	"closed_eye": preload("uid://d0xaj50jq38nb")
}
var view_password := false

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	super._ready()
	LoadingCircle.visible = false
	LoadingCircleAnim.play("rotate")
	if BackendClient.is_authenticated():
		get_tree().change_scene_to_file("res://Scenes/Home.tscn")

func _on_view_password_btn_pressed() -> void:
	view_password = not view_password
	if view_password:
		Password.secret = false
		_set_password_btn(textures_password["closed_eye"])
	else:
		Password.secret = true
		_set_password_btn(textures_password["open_eye"])

func _on_login_btn_pressed() -> void:
	_login()

func _set_password_btn(texture: Texture):
	ViewPasswordBtn.texture_normal = texture
	ViewPasswordBtn.texture_pressed = texture
	ViewPasswordBtn.texture_hover = texture

func _login() -> void:
	var user := User.text
	var password := Password.text
	# Validamos los datos primero
	ErrorTxt.visible = true
	if user.is_empty():
		ErrorTxt.text = "Usuario esta vacio"
		return
	if password.is_empty():
		ErrorTxt.text = "Contraseña esta vacio"
		return
	User.editable = false
	Password.editable = false
	ErrorTxt.visible = false
	LoginBtn.visible = false
	LoadingCircle.visible = true
	var response = await BackendClient.login(user, password)
	if not response.ok:
		LoadingCircle.visible = false
		LoginBtn.visible = true
		ErrorTxt.visible = true
		if response.status == 0:
			ErrorTxt.text = "%s: %d" % ["Sin respuesta", 503]
		else:
			var error_message = response.data.get("error", "")
			if error_message == "":
				error_message = response.data.get("detail", "")
			if error_message == "":
				error_message = response.data.get("message", "")
			if error_message == "":
				error_message = "Error desconocido"
			ErrorTxt.text = "%s: %d" % [str(error_message), response.status]
	User.editable = true
	Password.editable = true
