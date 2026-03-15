default:
	just --list

package name="pa.zip":
	rm -f "{{name}}"
	zip -r "{{name}}" pa1-3 -x "pa1-3/target/*" "pa1-3/out/*"
