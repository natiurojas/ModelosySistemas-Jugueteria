
@tool
class_name TextureRectCSS extends TextureRect

# ============================================================
# ENUMS
# ============================================================

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


# ============================================================
# CSS LAYOUT
# ============================================================

@export_group("CSS Layout")

@export_subgroup("Transform")

@export var screen_anchor: ScreenAnchor = ScreenAnchor.TOP_LEFT:
	set(value):
		screen_anchor = value
		_request_apply()

@export var css_position := Vector2.ZERO:
	set(value):
		css_position = value
		_request_apply()

@export var position_unit: PositionUnit = PositionUnit.PX:
	set(value):
		position_unit = value
		_request_apply()


@export_subgroup("Dimensions")

@export var css_size := Vector2(100, 100):
	set(value):
		css_size = value
		_request_apply()

@export var width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		width_unit = value
		_request_apply()

@export var height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		height_unit = value
		_request_apply()

@export var css_min_size := Vector2.ZERO:
	set(value):
		css_min_size = value
		_request_apply()

@export var css_max_size := Vector2(-1, -1):
	set(value):
		css_max_size = value
		_request_apply()

@export var min_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		min_width_unit = value
		_request_apply()

@export var min_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		min_height_unit = value
		_request_apply()

@export var max_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		max_width_unit = value
		_request_apply()

@export var max_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		max_height_unit = value
		_request_apply()

@export var aspect_ratio := 0.0:
	set(value):
		aspect_ratio = value
		_request_apply()


@export_subgroup("Box Model")

@export var css_margin := Vector4.ZERO:
	set(value):
		css_margin = value
		_request_apply()

@export var css_padding := Vector4.ZERO:
	set(value):
		css_padding = value
		_request_apply()

@export var box_sizing: BoxSizing = BoxSizing.BORDER_BOX:
	set(value):
		box_sizing = value
		_request_apply()


# ============================================================
# TEXTURE PROPERTIES
# ============================================================

@export_group("CSS Texture")

@export var object_fit: TextureRect.StretchMode = TextureRect.STRETCH_SCALE:
	set(value):
		object_fit = value
		stretch_mode = value

@export var flip_h_css := false:
	set(value):
		flip_h_css = value
		flip_h = value

@export var flip_v_css := false:
	set(value):
		flip_v_css = value
		flip_v = value


# ============================================================
# INTERNAL
# ============================================================

var applying_layout := false

signal layout_applied


# ============================================================
# LIFECYCLE
# ============================================================

func _ready() -> void:
	if not get_viewport().size_changed.is_connected(_on_viewport_size_changed):
		get_viewport().size_changed.connect(_on_viewport_size_changed)

	_connect_parent()

	stretch_mode = object_fit
	flip_h = flip_h_css
	flip_v = flip_v_css

	_apply_css()


func _connect_parent() -> void:
	var p := get_parent()

	if p is Control:
		if not p.resized.is_connected(_on_parent_resized):
			p.resized.connect(_on_parent_resized)


func _on_viewport_size_changed() -> void:
	_apply_css()


func _on_parent_resized() -> void:
	_apply_css()


# ============================================================
# UPDATE
# ============================================================

func _request_apply() -> void:
	if not is_inside_tree() or applying_layout:
		return

	call_deferred("_apply_css")


# ============================================================
# REFERENCE RECT
# ============================================================

func _get_viewport_reference() -> Vector2:
	if Engine.is_editor_hint():
		return Vector2(
			ProjectSettings.get_setting(
				"display/window/size/viewport_width"
			),
			ProjectSettings.get_setting(
				"display/window/size/viewport_height"
			)
		)

	return get_viewport_rect().size


func _get_reference_rect() -> Rect2:
	var p := get_parent()

	if p is Control:
		if p is TextureRectCSS:
			return p.get_css_content_rect()

		return Rect2(Vector2.ZERO, p.size)

	return Rect2(Vector2.ZERO, _get_viewport_reference())


func get_css_content_rect() -> Rect2:
	return Rect2(
		Vector2(css_padding.x, css_padding.y),
		Vector2(
			maxf(0.0, size.x - css_padding.x - css_padding.z),
			maxf(0.0, size.y - css_padding.y - css_padding.w)
		)
	)


# ============================================================
# POSITION
# ============================================================

func _resolve_position() -> Vector2:
	var reference := _get_reference_rect()
	var viewport := _get_viewport_reference()

	match position_unit:
		PositionUnit.PX:
			return css_position

		PositionUnit.PARENT_PERCENT:
			return Vector2(
				reference.size.x * css_position.x / 100.0,
				reference.size.y * css_position.y / 100.0
			)

		PositionUnit.VW:
			return viewport * css_position.x / 100.0

		PositionUnit.VH:
			return viewport * css_position.y / 100.0

	return css_position


# ============================================================
# SIZE
# ============================================================

func _resolve_dimension(
	value: float,
	unit: CSSUnit,
	parent_size: float,
	viewport: Vector2,
	is_width: bool
) -> float:

	match unit:
		CSSUnit.PX:
			return value

		CSSUnit.PARENT_PERCENT:
			return parent_size * value / 100.0

		CSSUnit.VW:
			return viewport.x * value / 100.0

		CSSUnit.VH:
			return viewport.y * value / 100.0

		CSSUnit.AUTO:
			var intrinsic := get_combined_minimum_size()
			return intrinsic.x if is_width else intrinsic.y

	return value

func _resolve_limit_axis(
	value: float,
	unit: CSSUnit,
	parent_size: float,
	viewport: Vector2,
	is_max: bool
) -> float:

	# AUTO
	if unit == CSSUnit.AUTO:
		return -1.0 if is_max else 0.0

	# -1 = unlimited
	if is_max and value < 0.0:
		return -1.0

	match unit:
		CSSUnit.PX:
			return value

		CSSUnit.PARENT_PERCENT:
			return parent_size * value / 100.0

		CSSUnit.VW:
			return viewport.x * value / 100.0

		CSSUnit.VH:
			return viewport.y * value / 100.0

	return value


func _resolve_limit_size(is_max: bool) -> Vector2:
	var reference := _get_reference_rect()
	var viewport := _get_viewport_reference()

	var values := css_max_size if is_max else css_min_size

	var width_unit_limit := (
		max_width_unit if is_max else min_width_unit
	)

	var height_unit_limit := (
		max_height_unit if is_max else min_height_unit
	)

	return Vector2(
		_resolve_limit_axis(
			values.x,
			width_unit_limit,
			reference.size.x,
			viewport,
			is_max
		),
		_resolve_limit_axis(
			values.y,
			height_unit_limit,
			reference.size.y,
			viewport,
			is_max
		)
	)

func _resolve_size() -> Vector2:
	var reference := _get_reference_rect()
	var viewport := _get_viewport_reference()

	var width := _resolve_dimension(
		css_size.x, width_unit,
		reference.size.x, viewport, true
	)

	var height := _resolve_dimension(
		css_size.y, height_unit,
		reference.size.y, viewport, false
	)

	if box_sizing == BoxSizing.CONTENT_BOX:
		if width_unit != CSSUnit.AUTO:
			width += css_padding.x + css_padding.z

		if height_unit != CSSUnit.AUTO:
			height += css_padding.y + css_padding.w

	var result := Vector2(width, height)

	var min_size := _resolve_limit_size(false)
	var max_size := _resolve_limit_size(true)

	result.x = maxf(result.x, min_size.x)
	result.y = maxf(result.y, min_size.y)

	if max_size.x >= 0.0:
		result.x = minf(result.x, max_size.x)

	if max_size.y >= 0.0:
		result.y = minf(result.y, max_size.y)

	if aspect_ratio > 0.0:
		result.y = result.x / aspect_ratio

	return result

# ============================================================
# ANCHOR
# ============================================================

func _get_screen_position() -> Vector2:
	var reference := _get_reference_rect()
	var offset := _resolve_position()

	var w := size.x
	var h := size.y

	var center := (reference.size - size) / 2.0

	match screen_anchor:
		ScreenAnchor.TOP_LEFT:
			return reference.position + offset + Vector2(css_margin.x, css_margin.y)

		ScreenAnchor.TOP_CENTER:
			return reference.position + Vector2(
				center.x + offset.x,
				offset.y + css_margin.y
			)

		ScreenAnchor.TOP_RIGHT:
			return reference.position + Vector2(
				reference.size.x - w - offset.x - css_margin.z,
				offset.y + css_margin.y
			)

		ScreenAnchor.CENTER_LEFT:
			return reference.position + Vector2(
				offset.x + css_margin.x,
				center.y + offset.y
			)

		ScreenAnchor.CENTER:
			return reference.position + center + offset

		ScreenAnchor.CENTER_RIGHT:
			return reference.position + Vector2(
				reference.size.x - w - offset.x - css_margin.z,
				center.y + offset.y
			)

		ScreenAnchor.BOTTOM_LEFT:
			return reference.position + Vector2(
				offset.x + css_margin.x,
				reference.size.y - h - offset.y - css_margin.w
			)

		ScreenAnchor.BOTTOM_CENTER:
			return reference.position + Vector2(
				center.x + offset.x,
				reference.size.y - h - offset.y - css_margin.w
			)

		ScreenAnchor.BOTTOM_RIGHT:
			return reference.position + reference.size - size - offset - Vector2(
				css_margin.z,
				css_margin.w
			)

	return reference.position + offset


# ============================================================
# APPLY
# ============================================================

func _apply_css() -> void:
	if applying_layout or not is_inside_tree():
		return

	applying_layout = true

	anchor_left = 0.0
	anchor_top = 0.0
	anchor_right = 0.0
	anchor_bottom = 0.0

	# Aplicar CSS MIN SIZE
	custom_minimum_size = _resolve_limit_size(false)

	# Calcular dimensiones
	size = _resolve_size()
	
	if not get_parent() is Container:
		position = _get_screen_position()

	applying_layout = false

	layout_applied.emit()
