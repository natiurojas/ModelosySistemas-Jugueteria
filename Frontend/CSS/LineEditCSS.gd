
@tool
class_name LineEditCSS
extends LineEdit

enum ScreenAnchor {
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

enum CSSUnit {
	PX,
	PARENT_PERCENT,
	VW,
	VH,
	AUTO
}

enum PositionUnit {
	PX,
	PARENT_PERCENT,
	VW,
	VH
}

enum BoxSizing {
	CONTENT_BOX,
	BORDER_BOX
}

@export_group("CSS Layout")

@export var css_anchor: ScreenAnchor = ScreenAnchor.TOP_LEFT:
	set(value):
		css_anchor = value
		_apply_css()

@export var css_position: Vector2 = Vector2.ZERO:
	set(value):
		css_position = value
		_apply_css()

@export var css_position_unit: PositionUnit = PositionUnit.PX:
	set(value):
		css_position_unit = value
		_apply_css()

@export var css_size: Vector2 = Vector2(200, 40):
	set(value):
		css_size = value
		_apply_css()

@export var css_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_width_unit = value
		_apply_css()

@export var css_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_height_unit = value
		_apply_css()

@export var css_margin: Vector4 = Vector4.ZERO:
	set(value):
		css_margin = value
		_apply_css()

@export var css_padding: Vector4 = Vector4(8, 4, 8, 4):
	set(value):
		css_padding = value
		_update_styles()
		_apply_css()

@export var css_min_size: Vector2 = Vector2.ZERO:
	set(value):
		css_min_size = value
		_apply_css()

@export var css_max_size: Vector2 = Vector2(99999, 99999):
	set(value):
		css_max_size = value
		_apply_css()

@export var css_min_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_min_width_unit = value
		_apply_css()

@export var css_min_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_min_height_unit = value
		_apply_css()

@export var css_max_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_max_width_unit = value
		_apply_css()

@export var css_max_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		css_max_height_unit = value
		_apply_css()

@export var css_aspect_ratio: float = 0.0:
	set(value):
		css_aspect_ratio = value
		_apply_css()

@export var css_box_sizing: BoxSizing = BoxSizing.BORDER_BOX:
	set(value):
		css_box_sizing = value
		_apply_css()


@export_group("CSS Appearance")

@export var css_background_color: Color = Color.WHITE:
	set(value):
		css_background_color = value
		_update_styles()

@export var css_text_color: Color = Color.BLACK:
	set(value):
		css_text_color = value
		add_theme_color_override("font_color", value)

@export var css_placeholder_color: Color = Color.GRAY:
	set(value):
		css_placeholder_color = value
		add_theme_color_override("font_placeholder_color", value)

@export var css_font_size: int = 16:
	set(value):
		css_font_size = value
		add_theme_font_size_override("font_size", value)

@export var css_border_color: Color = Color(0.7, 0.7, 0.7):
	set(value):
		css_border_color = value
		_update_styles()

@export var css_border_width: float = 1.0:
	set(value):
		css_border_width = value
		_update_styles()

@export var css_border_radius: float = 6.0:
	set(value):
		css_border_radius = value
		_update_styles()

@export var css_focus_border_color: Color = Color(0.2, 0.5, 1.0):
	set(value):
		css_focus_border_color = value
		_update_styles()

@export var css_selection_color: Color = Color(0.25, 0.5, 1.0, 0.5):
	set(value):
		css_selection_color = value
		add_theme_color_override("selection_color", value)


@export_group("CSS Text")

@export var css_placeholder: String = "":
	set(value):
		css_placeholder = value
		placeholder_text = value

@export var css_readonly: bool = false:
	set(value):
		css_readonly = value
		editable = not value

@export var css_max_length: int = 0:
	set(value):
		css_max_length = value
		max_length = value

@export var css_clear_button: bool = false:
	set(value):
		css_clear_button = value
		clear_button_enabled = value

@export var css_secret: bool = false:
	set(value):
		css_secret = value
		secret = value

@export var css_secret_character: String = "•":
	set(value):
		css_secret_character = value
		secret_character = value

@export var css_expand_to_text: bool = false:
	set(value):
		css_expand_to_text = value
		expand_to_text_length = value


var _parent_control: Control


func _ready() -> void:
	_parent_control = get_parent() as Control

	if _parent_control:
		_parent_control.resized.connect(_apply_css)

	_update_styles()
	_apply_css()


func _update_styles() -> void:
	var normal := _create_style(
		css_background_color,
		css_border_color
	)

	var focus := _create_style(
		css_background_color,
		css_focus_border_color
	)

	var read_only := _create_style(
		css_background_color.darkened(0.05),
		css_border_color
	)

	add_theme_stylebox_override("normal", normal)
	add_theme_stylebox_override("focus", focus)
	add_theme_stylebox_override("read_only", read_only)


func _create_style(bg: Color, border: Color) -> StyleBoxFlat:
	var style := StyleBoxFlat.new()

	style.bg_color = bg

	style.set_border_width_all(int(css_border_width))
	style.border_color = border

	style.set_corner_radius_all(int(css_border_radius))

	style.content_margin_left = css_padding.x
	style.content_margin_top = css_padding.y
	style.content_margin_right = css_padding.z
	style.content_margin_bottom = css_padding.w

	return style

func _resolve_limit_axis(
	value: float,
	unit: CSSUnit,
	parent_value: float,
	viewport_size: Vector2,
	is_max: bool,
	is_width: bool
) -> float:

	if unit == CSSUnit.AUTO:
		return -1.0 if is_max else 0.0

	if is_max and value < 0:
		return -1.0

	match unit:
		CSSUnit.PX:
			return value

		CSSUnit.PARENT_PERCENT:
			return parent_value * value / 100.0

		CSSUnit.VW:
			return viewport_size.x * value / 100.0

		CSSUnit.VH:
			return viewport_size.y * value / 100.0

	return value


func _resolve_limit_size(
	is_max: bool,
	reference_size: Vector2,
	viewport_size: Vector2
) -> Vector2:

	var values := css_max_size if is_max else css_min_size

	var width_unit := (
		css_max_width_unit if is_max else css_min_width_unit
	)

	var height_unit := (
		css_max_height_unit if is_max else css_min_height_unit
	)

	return Vector2(
		_resolve_limit_axis(
			values.x,
			width_unit,
			reference_size.x,
			viewport_size,
			is_max,
			true
		),
		_resolve_limit_axis(
			values.y,
			height_unit,
			reference_size.y,
			viewport_size,
			is_max,
			false
		)
	)

func _apply_css() -> void:
	if not is_inside_tree():
		return

	var viewport_size := get_viewport_rect().size
	var reference_size := viewport_size

	if _parent_control:
		reference_size = _parent_control.size

	var width := _resolve_dimension(
		css_size.x,
		css_width_unit,
		reference_size.x,
		viewport_size.x,
		true
	)

	var height := _resolve_dimension(
		css_size.y,
		css_height_unit,
		reference_size.y,
		viewport_size.y,
		false
	)

	if css_aspect_ratio > 0.0:
		height = width / css_aspect_ratio

	if css_box_sizing == BoxSizing.CONTENT_BOX:
		width += css_padding.x + css_padding.z + css_border_width * 2.0
		height += css_padding.y + css_padding.w + css_border_width * 2.0

	var min_size := _resolve_limit_size(
		false,
		reference_size,
		viewport_size
	)

	var max_size := _resolve_limit_size(
		true,
		reference_size,
		viewport_size
	)

	width = maxf(width, min_size.x)
	height = maxf(height, min_size.y)

	if max_size.x >= 0.0:
		width = minf(width, max_size.x)

	if max_size.y >= 0.0:
		height = minf(height, max_size.y)

	var final_size := Vector2(width, height)
	size = final_size

	if get_parent() is Container:
		return
	
	var pos := _resolve_position(
		css_position,
		css_position_unit,
		reference_size,
		viewport_size
	)

	var anchor_offset := _get_anchor_offset(
		css_anchor,
		reference_size,
		final_size
	)

	pos += anchor_offset

	pos.x += css_margin.x - css_margin.z
	pos.y += css_margin.y - css_margin.w

	anchor_left = 0
	anchor_top = 0
	anchor_right = 0
	anchor_bottom = 0

	position = pos


func _resolve_dimension(
	value: float,
	unit: CSSUnit,
	parent_value: float,
	viewport_value: float,
	is_width: bool
) -> float:
	match unit:
		CSSUnit.PX:
			return value

		CSSUnit.PARENT_PERCENT:
			return parent_value * value / 100.0

		CSSUnit.VW:
			return get_viewport_rect().size.x * value / 100.0

		CSSUnit.VH:
			return get_viewport_rect().size.y * value / 100.0

		CSSUnit.AUTO:
			return get_combined_minimum_size().x if is_width else get_combined_minimum_size().y

	return value


func _resolve_position(
	value: Vector2,
	unit: PositionUnit,
	parent_size: Vector2,
	viewport_size: Vector2
) -> Vector2:
	match unit:
		PositionUnit.PX:
			return value

		PositionUnit.PARENT_PERCENT:
			return Vector2(
				parent_size.x * value.x / 100.0,
				parent_size.y * value.y / 100.0
			)

		PositionUnit.VW:
			return Vector2(
				viewport_size.x * value.x / 100.0,
				viewport_size.x * value.y / 100.0
			)

		PositionUnit.VH:
			return Vector2(
				viewport_size.y * value.x / 100.0,
				viewport_size.y * value.y / 100.0
			)

	return value


func _get_anchor_offset(
	anchor: ScreenAnchor,
	reference_size: Vector2,
	element_size: Vector2
) -> Vector2:
	match anchor:
		ScreenAnchor.TOP_LEFT:
			return Vector2.ZERO

		ScreenAnchor.TOP_CENTER:
			return Vector2(reference_size.x / 2.0 - element_size.x / 2.0, 0)

		ScreenAnchor.TOP_RIGHT:
			return Vector2(reference_size.x - element_size.x, 0)

		ScreenAnchor.CENTER_LEFT:
			return Vector2(0, reference_size.y / 2.0 - element_size.y / 2.0)

		ScreenAnchor.CENTER:
			return (reference_size - element_size) / 2.0

		ScreenAnchor.CENTER_RIGHT:
			return Vector2(
				reference_size.x - element_size.x,
				reference_size.y / 2.0 - element_size.y / 2.0
			)

		ScreenAnchor.BOTTOM_LEFT:
			return Vector2(0, reference_size.y - element_size.y)

		ScreenAnchor.BOTTOM_CENTER:
			return Vector2(
				reference_size.x / 2.0 - element_size.x / 2.0,
				reference_size.y - element_size.y
			)

		ScreenAnchor.BOTTOM_RIGHT:
			return reference_size - element_size

	return Vector2.ZERO
