
@tool
class_name TextEditCSS
extends TextEdit

enum CSSUnit {
	PX,
	PARENT_PERCENT,
	VW,
	VH,
	AUTO
}

enum PositionAnchor {
	TOP_LEFT,
	TOP_CENTER,
	TOP_RIGHT,
	CENTER_LEFT,
	CENTER,
	CENTER_RIGHT,
	BOTTOM_LEFT,
	BOTTOM_CENTER,
	BOTTOM_RIGHT
}

@export_group("CSS Layout")

@export var css_width: float = -1.0:
	set(value):
		css_width = value
		_apply_css()

@export var css_height: float = -1.0:
	set(value):
		css_height = value
		_apply_css()

@export var css_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_width_unit = value
		_apply_css()

@export var css_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_height_unit = value
		_apply_css()

# Vector4(left, top, right, bottom)
@export var css_margin: Vector4 = Vector4.ZERO:
	set(value):
		css_margin = value
		_apply_css()

@export var css_padding: Vector4 = Vector4(8, 8, 8, 8):
	set(value):
		css_padding = value
		_update_style()

@export var css_anchor: PositionAnchor = PositionAnchor.TOP_LEFT:
	set(value):
		css_anchor = value
		_apply_css()

@export_group("CSS Appearance")

@export var css_background: Color = Color("#252525"):
	set(value):
		css_background = value
		_update_style()

@export var css_text_color: Color = Color.WHITE:
	set(value):
		css_text_color = value
		add_theme_color_override("font_color", value)

@export var css_placeholder_color: Color = Color.GRAY:
	set(value):
		css_placeholder_color = value
		add_theme_color_override("font_placeholder_color", value)

@export var css_border_color: Color = Color("#555555"):
	set(value):
		css_border_color = value
		_update_style()

@export var css_border_width: float = 1.0:
	set(value):
		css_border_width = value
		_update_style()

@export var css_border_radius: float = 5.0:
	set(value):
		css_border_radius = value
		_update_style()

@export var css_font_size: int = 16:
	set(value):
		css_font_size = value
		add_theme_font_size_override("font_size", value)


func _ready() -> void:
	_update_style()
	_apply_css()

	if get_parent() is Control:
		get_parent().resized.connect(_apply_css)


func _notification(what: int) -> void:
	if what == NOTIFICATION_RESIZED:
		if is_inside_tree():
			_apply_css()


func _get_reference_size() -> Vector2:
	if get_parent() is Control:
		return get_parent().size

	return get_viewport_rect().size


func _resolve_dimension(value: float, unit: CSSUnit, axis: int) -> float:
	var reference := _get_reference_size()
	var viewport := get_viewport_rect().size

	match unit:
		CSSUnit.PX:
			return value
		CSSUnit.PARENT_PERCENT:
			return reference[axis] * value / 100.0
		CSSUnit.VW:
			return viewport.x * value / 100.0
		CSSUnit.VH:
			return viewport.y * value / 100.0
		CSSUnit.AUTO:
			return get_combined_minimum_size()[axis]

	return value


func _apply_css() -> void:
	if not is_inside_tree():
		return

	var reference := _get_reference_size()

	var w := _resolve_dimension(css_width, css_width_unit, 0)
	var h := _resolve_dimension(css_height, css_height_unit, 1)

	if css_width < 0:
		w = get_combined_minimum_size().x

	if css_height < 0:
		h = get_combined_minimum_size().y

	size = Vector2(maxf(0.0, w), maxf(0.0, h))

	var x := css_margin.x
	var y := css_margin.y

	match css_anchor:
		PositionAnchor.TOP_CENTER, PositionAnchor.CENTER, PositionAnchor.BOTTOM_CENTER:
			x += (reference.x - size.x) / 2.0
		PositionAnchor.TOP_RIGHT, PositionAnchor.CENTER_RIGHT, PositionAnchor.BOTTOM_RIGHT:
			x += reference.x - size.x

	match css_anchor:
		PositionAnchor.CENTER_LEFT, PositionAnchor.CENTER, PositionAnchor.CENTER_RIGHT:
			y += (reference.y - size.y) / 2.0
		PositionAnchor.BOTTOM_LEFT, PositionAnchor.BOTTOM_CENTER, PositionAnchor.BOTTOM_RIGHT:
			y += reference.y - size.y

	position = Vector2(x, y)


func _update_style() -> void:
	var style := StyleBoxFlat.new()

	style.bg_color = css_background
	style.border_color = css_border_color
	style.set_border_width_all(roundi(css_border_width))
	style.set_corner_radius_all(roundi(css_border_radius))

	style.content_margin_left = css_padding.x
	style.content_margin_top = css_padding.y
	style.content_margin_right = css_padding.z
	style.content_margin_bottom = css_padding.w

	add_theme_stylebox_override("normal", style)
	add_theme_stylebox_override("focus", style)
	add_theme_stylebox_override("read_only", style)
